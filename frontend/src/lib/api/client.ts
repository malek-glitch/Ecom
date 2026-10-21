import { error } from '@sveltejs/kit';
import createClient from 'openapi-fetch';
import { session } from '$lib/auth/session.svelte';
import { API_BASE } from './base';
import { ApiError, errorMessage } from './errors';
import type { paths } from './schema';

/**
 * Typed API client. Paths, parameters and bodies are checked against the backend's OpenAPI
 * spec (`npm run gen:api` after changing a backend endpoint).
 *
 *   const notes = await call(api.GET('/api/v1/notes', { params: { query: { q } } }));
 */
export const api = createClient<paths>({
	baseUrl: API_BASE,
	fetch: (request) => session.fetch(request),
});

/** Unwraps an openapi-fetch result: returns the data, or throws an [ApiError]. */
export async function call<T>(request: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T> {
	let result: { data?: T; error?: unknown; response: Response };
	try {
		result = await request;
	} catch {
		throw ApiError.network();
	}
	if (!result.response.ok) throw ApiError.fromBody(result.response.status, result.error);
	return result.data as T;
}

/**
 * [call] for load functions: a failure shows SvelteKit's error page with the right status and
 * a translated message (a missing note is a 404 page, not a generic 500).
 */
export async function callForPage<T>(request: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T> {
	try {
		return await call(request);
	} catch (failure) {
		if (failure instanceof ApiError)
			error(failure.status || 503, { message: errorMessage(failure), code: failure.code });
		throw failure;
	}
}
