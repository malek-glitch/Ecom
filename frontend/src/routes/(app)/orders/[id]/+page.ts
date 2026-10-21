import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ params, depends }) => {
	depends('app:order');
	return { order: await callForPage(api.GET('/api/v1/orders/{id}', { params: { path: { id: params.id } } })) };
}) satisfies PageLoad;
