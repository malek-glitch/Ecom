import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ params, depends, parent }) => {
	await parent();
	depends('app:admin-order');
	return {
		order: await callForPage(api.GET('/api/v1/admin/shop/orders/{id}', { params: { path: { id: params.id } } })),
	};
}) satisfies PageLoad;
