import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ params, depends }) => {
	depends('app:note');
	return { note: await callForPage(api.GET('/api/v1/notes/{id}', { params: { path: { id: params.id } } })) };
}) satisfies PageLoad;
