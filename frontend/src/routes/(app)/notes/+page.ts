import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import type { PageLoad } from './$types';

const PAGE_SIZE = 12;

/** The URL holds the search and the page, so results survive reloads and can be shared. */
export const load = (async ({ url, depends }) => {
	depends('app:notes');
	const q = url.searchParams.get('q')?.trim() ?? '';
	const notes = await callForPage(
		api.GET('/api/v1/notes', {
			params: { query: { q: q || undefined, page: intParam(url, 'page'), size: PAGE_SIZE } },
		}),
	);
	return { notes, q };
}) satisfies PageLoad;
