import { api, call } from '$lib/api/client';
import type { PageLoad } from './$types';

// Not awaited: the page renders at once and each card fills in when its data arrives.
// A card that fails shows a dash instead of failing the whole page.
export const load = (() => ({
	sessionCount: call(api.GET('/api/v1/me/sessions'))
		.then((sessions) => sessions.length)
		.catch(() => null),
	// [example:notes]
	noteCount: call(api.GET('/api/v1/notes', { params: { query: { size: 1 } } }))
		.then((page) => page.totalItems)
		.catch(() => null),
	// [/example:notes]
})) satisfies PageLoad;
