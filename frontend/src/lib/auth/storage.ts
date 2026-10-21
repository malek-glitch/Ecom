import { SESSION_STORAGE_KEY } from '$lib/config';

export interface StoredSession {
	accessToken: string;
	accessTokenExpiresAt: string;
	refreshToken: string;
	refreshTokenExpiresAt: string;
}

/**
 * Where tokens are kept. The default is localStorage. A Capacitor build can swap in
 * @capacitor/preferences (see docs/MOBILE.md) without touching the rest of the app.
 */
export interface TokenStorage {
	load(): StoredSession | null;
	save(session: StoredSession): void;
	clear(): void;
	/** Called when another tab changes the stored session. Returns an unsubscribe function. */
	subscribe(listener: () => void): () => void;
}

export class LocalTokenStorage implements TokenStorage {
	constructor(private readonly key = SESSION_STORAGE_KEY) {}

	load(): StoredSession | null {
		try {
			const raw = localStorage.getItem(this.key);
			return raw ? (JSON.parse(raw) as StoredSession) : null;
		} catch {
			return null;
		}
	}

	save(session: StoredSession) {
		localStorage.setItem(this.key, JSON.stringify(session));
	}

	clear() {
		localStorage.removeItem(this.key);
	}

	subscribe(listener: () => void) {
		const handler = (event: StorageEvent) => {
			if (event.key === this.key) listener();
		};
		window.addEventListener('storage', handler);
		return () => window.removeEventListener('storage', handler);
	}
}

/** Keeps nothing: used during server-side rendering and in tests. */
export class MemoryTokenStorage implements TokenStorage {
	private value: StoredSession | null = null;

	load() {
		return this.value;
	}

	save(session: StoredSession) {
		this.value = session;
	}

	clear() {
		this.value = null;
	}

	subscribe() {
		return () => {};
	}
}
