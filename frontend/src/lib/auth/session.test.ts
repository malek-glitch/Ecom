import { describe, expect, it, vi } from 'vitest';
import { Session, type TokenResponse } from './session.svelte';
import { MemoryTokenStorage } from './storage';

vi.mock('$app/environment', () => ({ browser: false }));

const user = {
	id: '00000000-0000-7000-8000-000000000001',
	email: 'ada@example.com',
	fullName: 'Ada Lovelace',
	role: 'USER' as const,
	locale: 'en',
	avatarUrl: null,
	emailVerified: true,
	hasPassword: true,
	googleLinked: false,
	createdAt: '2026-01-01T00:00:00Z',
};

function tokens(suffix: string, accessExpiresInMs = 15 * 60_000): TokenResponse {
	return {
		accessToken: `access-${suffix}`,
		accessTokenExpiresAt: new Date(Date.now() + accessExpiresInMs).toISOString(),
		refreshToken: `refresh-${suffix}`,
		refreshTokenExpiresAt: new Date(Date.now() + 30 * 86_400_000).toISOString(),
		tokenType: 'Bearer',
		user,
	};
}

const json = (body: unknown, status = 200) =>
	new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

describe('Session', () => {
	it('shares one refresh between concurrent callers', async () => {
		let refreshes = 0;
		const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
			const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
			if (url.endsWith('/api/v1/auth/refresh')) {
				refreshes++;
				await new Promise((resolve) => setTimeout(resolve, 10));
				return json(tokens('2'));
			}
			return json({});
		});
		const session = new Session(new MemoryTokenStorage(), fetchMock as typeof fetch);
		session.signIn(tokens('1', 1_000)); // expires within the refresh margin

		const results = await Promise.all([session.accessToken(), session.accessToken(), session.accessToken()]);

		expect(refreshes).toBe(1);
		expect(results).toEqual(['access-2', 'access-2', 'access-2']);
	});

	it('signs out when the refresh token is rejected', async () => {
		const fetchMock = vi.fn(async () => json({ code: 'auth.refresh_reused' }, 401));
		const session = new Session(new MemoryTokenStorage(), fetchMock as typeof fetch);
		session.signIn(tokens('1', 1_000));

		expect(await session.refresh()).toBe(false);
		expect(session.isAuthenticated).toBe(false);
		expect(session.user).toBeNull();
	});

	it('keeps the session when the network is down', async () => {
		const fetchMock = vi.fn(async () => {
			throw new TypeError('fetch failed');
		});
		const session = new Session(new MemoryTokenStorage(), fetchMock as typeof fetch);
		session.signIn(tokens('1', 1_000));

		expect(await session.refresh()).toBe(false);
		expect(session.isAuthenticated).toBe(true);
	});

	it('retries once with a fresh token when the server rejects the access token', async () => {
		const seen: (string | null)[] = [];
		const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
			if (!(input instanceof Request)) return json(tokens('2')); // the refresh call (url + init)
			seen.push(input.headers.get('Authorization'));
			return seen.length === 1 ? json({ code: 'auth.invalid_token' }, 401) : json({ ok: true });
		});
		const session = new Session(new MemoryTokenStorage(), fetchMock as typeof fetch);
		session.signIn(tokens('1'));

		const response = await session.fetch(new Request('http://localhost/api/v1/notes'));

		expect(response.status).toBe(200);
		expect(seen).toEqual(['Bearer access-1', 'Bearer access-2']);
	});

	it('does not refresh for other 401s', async () => {
		const fetchMock = vi.fn(async () => json({ code: 'auth.invalid_credentials' }, 401));
		const session = new Session(new MemoryTokenStorage(), fetchMock as typeof fetch);
		session.signIn(tokens('1'));

		const response = await session.fetch(new Request('http://localhost/api/v1/me/password', { method: 'PUT' }));

		expect(response.status).toBe(401);
		expect(fetchMock).toHaveBeenCalledTimes(1);
	});
});
