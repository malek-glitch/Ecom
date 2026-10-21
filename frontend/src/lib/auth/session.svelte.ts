import { browser } from '$app/environment';
import { apiUrl } from '$lib/api/base';
import type { components } from '$lib/api/schema';
import { LocalTokenStorage, MemoryTokenStorage, type StoredSession, type TokenStorage } from './storage';

export type Me = components['schemas']['MeResponse'];
export type TokenResponse = components['schemas']['TokenResponse'];

/** Refresh this long before the access token expires, so requests never race its expiry. */
const EXPIRY_MARGIN_MS = 60_000;
const REFRESH_LOCK = 'liftoff-session-refresh';

const expiresSoon = (iso: string) => Date.parse(iso) - Date.now() < EXPIRY_MARGIN_MS;

/** The user id inside an access token (read only, never trusted: the server verifies tokens). */
function subject(token: string): string | null {
	try {
		const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
		return (JSON.parse(atob(payload)) as { sub?: string }).sub ?? null;
	} catch {
		return null;
	}
}

/**
 * The signed-in user and their tokens, as reactive state.
 *
 * Tokens live in [TokenStorage] (localStorage by default). Access tokens are short-lived;
 * the refresh token is rotated by the server on every use, so refreshes must never run twice
 * with the same token: concurrent callers share one in-flight refresh, and tabs coordinate
 * through a Web Lock, re-reading storage in case another tab already refreshed.
 */
export class Session {
	tokens = $state<StoredSession | null>(null);
	user = $state<Me | null>(null);

	readonly isAuthenticated = $derived(this.tokens !== null);
	readonly isAdmin = $derived(this.user?.role === 'ADMIN');

	#storage: TokenStorage;
	#fetch: typeof fetch;
	#hydrated = false;
	#init: Promise<void> | null = null;
	#refreshing: Promise<boolean> | null = null;

	constructor(storage: TokenStorage, fetchImpl: typeof fetch = (...args) => globalThis.fetch(...args)) {
		this.#storage = storage;
		this.#fetch = fetchImpl;
	}

	/** Loads stored tokens and the current user, once. Safe to call from every load function. */
	init(): Promise<void> {
		return (this.#init ??= this.#initialize());
	}

	async #initialize() {
		this.#hydrate();
		if (this.tokens) await this.reloadUser();
	}

	/** Reads the stored tokens (synchronously, once), so API calls never race init(). */
	#hydrate() {
		if (this.#hydrated) return;
		this.#hydrated = true;
		this.tokens = this.#storage.load();
		// Another tab signed in, refreshed or signed out.
		this.#storage.subscribe(() => {
			const previous = this.tokens;
			this.tokens = this.#storage.load();
			if (!this.tokens) this.user = null;
			else if (!previous || subject(previous.accessToken) !== subject(this.tokens.accessToken)) void this.reloadUser();
		});
	}

	/** Stores a fresh token pair (login, register, refresh, password reset). */
	signIn(response: TokenResponse) {
		this.#hydrate();
		const tokens: StoredSession = {
			accessToken: response.accessToken,
			accessTokenExpiresAt: response.accessTokenExpiresAt,
			refreshToken: response.refreshToken,
			refreshTokenExpiresAt: response.refreshTokenExpiresAt,
		};
		this.#storage.save(tokens);
		this.tokens = tokens;
		this.user = response.user;
	}

	/** Revokes the session on the server (best effort) and forgets it locally. */
	async signOut() {
		const refreshToken = this.tokens?.refreshToken;
		this.clear();
		if (refreshToken) {
			await this.#fetch(apiUrl('/api/v1/auth/logout'), {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ refreshToken }),
			}).catch(() => undefined);
		}
	}

	clear() {
		this.#storage.clear();
		this.tokens = null;
		this.user = null;
	}

	async reloadUser() {
		const response = await this.fetch(new Request(apiUrl('/api/v1/me'))).catch(() => null);
		if (response?.ok) this.user = (await response.json()) as Me;
		else if (response && (response.status === 401 || response.status === 404)) this.clear();
	}

	/** A valid access token, refreshed first if it is about to expire. */
	async accessToken(): Promise<string | null> {
		this.#hydrate();
		if (!this.tokens) return null;
		if (expiresSoon(this.tokens.accessTokenExpiresAt)) await this.refresh();
		return this.tokens?.accessToken ?? null;
	}

	/** Single-flight: concurrent callers share one refresh. Resolves false when the session is over. */
	refresh(): Promise<boolean> {
		return (this.#refreshing ??= this.#refreshAcrossTabs().finally(() => (this.#refreshing = null)));
	}

	async #refreshAcrossTabs(): Promise<boolean> {
		const locks = browser && 'locks' in navigator ? navigator.locks : null;
		return locks ? locks.request(REFRESH_LOCK, () => this.#refreshNow()) : this.#refreshNow();
	}

	async #refreshNow(): Promise<boolean> {
		const stored = this.#storage.load();
		// Another tab refreshed while we waited for the lock: use its tokens instead of
		// spending our (now rotated) refresh token, which the server would treat as theft.
		if (stored && stored.refreshToken !== this.tokens?.refreshToken && !expiresSoon(stored.accessTokenExpiresAt)) {
			this.tokens = stored;
			return true;
		}
		const refreshToken = stored?.refreshToken ?? this.tokens?.refreshToken;
		if (!refreshToken) {
			this.clear();
			return false;
		}
		try {
			const response = await this.#fetch(apiUrl('/api/v1/auth/refresh'), {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ refreshToken }),
			});
			if (response.ok) {
				this.signIn((await response.json()) as TokenResponse);
				return true;
			}
			if (response.status === 401 || response.status === 403) this.clear();
			return false;
		} catch {
			return false; // offline: keep the session and try again later
		}
	}

	/**
	 * fetch() for API calls: adds the bearer token, refreshes it when it's about to expire,
	 * and retries once if the server says the token is no longer valid.
	 */
	async fetch(request: Request): Promise<Response> {
		const retry = request.clone();
		const token = await this.accessToken();
		if (token) request.headers.set('Authorization', `Bearer ${token}`);
		const response = await this.#fetch(request);
		if (response.status !== 401 || !token) return response;

		const code = await response
			.clone()
			.json()
			.then((body: { code?: string }) => body.code)
			.catch(() => undefined);
		if (code !== 'auth.invalid_token' || !(await this.refresh()) || !this.tokens) return response;
		retry.headers.set('Authorization', `Bearer ${this.tokens.accessToken}`);
		return this.#fetch(retry);
	}
}

/** The app-wide session. Server-side rendering gets an empty in-memory one. */
export const session = new Session(browser ? new LocalTokenStorage() : new MemoryTokenStorage());
