import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ params, depends, parent }) => {
	await parent();
	depends('app:admin-product');
	const [product, categories] = await Promise.all([
		callForPage(api.GET('/api/v1/admin/shop/products/{id}', { params: { path: { id: params.id } } })),
		callForPage(api.GET('/api/v1/admin/shop/categories')),
	]);
	return { product, categories };
}) satisfies PageLoad;
