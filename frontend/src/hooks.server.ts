import type { Handle, HandleFetch } from '@sveltejs/kit';
import { sequence } from '@sveltejs/kit/hooks';
import { env } from '$env/dynamic/private';
import { paraglideMiddleware } from '$lib/paraglide/server';
import { getTextDirection } from '$lib/paraglide/runtime';

/** Resolves the language (cookie → Accept-Language → default) and sets <html lang dir>. */
const i18n: Handle = ({ event, resolve }) =>
	paraglideMiddleware(event.request, ({ request, locale }) => {
		event.request = request;
		return resolve(event, {
			transformPageChunk: ({ html }) =>
				html.replace('%paraglide.lang%', locale).replace('%paraglide.dir%', getTextDirection(locale)),
			// openapi-fetch (the typed API client in server-side loads) reads these response headers.
			filterSerializedResponseHeaders: (name) => name === 'content-length' || name === 'content-type',
		});
	});

const securityHeaders: Handle = async ({ event, resolve }) => {
	const response = await resolve(event);
	response.headers.set('X-Content-Type-Options', 'nosniff');
	response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');
	response.headers.set('X-Frame-Options', 'DENY');
	response.headers.set('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
	return response;
};

export const handle = sequence(securityHeaders, i18n);

const SSR_API_TIMEOUT_MS = 3_000;

/**
 * Server-side loads call the API with relative URLs (`/api/...`), exactly like the browser.
 * Here those calls go straight to the backend container (API_INTERNAL_URL) instead of
 * looping out through the public domain, which often hangs inside Docker (no hairpin NAT).
 * The short timeout keeps a misconfiguration from stalling every page.
 */
export const handleFetch: HandleFetch = async ({ event, request, fetch }) => {
	const url = new URL(request.url);
	if (url.origin === event.url.origin && url.pathname.startsWith('/api/') && request.method === 'GET') {
		const backend = env.API_INTERNAL_URL || 'http://localhost:8080';
		const headers = new Headers(request.headers);
		headers.delete('origin');
		// The platform fetch, not the one SvelteKit passes in: that one adds `Origin: <this site>`
		// to requests for other origins, and the backend's CORS check refuses an origin it
		// doesn't list (403). This is a server-to-server call, where CORS has no role.
		return globalThis.fetch(new URL(url.pathname + url.search, backend), {
			headers,
			signal: AbortSignal.timeout(SSR_API_TIMEOUT_MS),
		});
	}
	return fetch(request);
};
