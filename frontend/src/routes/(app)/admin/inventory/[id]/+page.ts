import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import type { PageLoad } from './$types';

export const load = (async ({ params, url, depends, parent }) => {
	await parent();
	depends('app:inventory');
	const [row, movements, summary] = await Promise.all([
		callForPage(api.GET('/api/v1/admin/inventory/products/{id}', { params: { path: { id: params.id } } })),
		callForPage(
			api.GET('/api/v1/admin/inventory/movements', {
				params: { query: { productId: params.id, page: intParam(url, 'page'), size: 25 } },
			}),
		),
		callForPage(api.GET('/api/v1/admin/inventory/summary')),
	]);
	return { row, movements, summary };
}) satisfies PageLoad;
