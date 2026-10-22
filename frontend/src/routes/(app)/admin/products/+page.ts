import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import type { PageLoad } from './$types';

const STATUSES = ['ACTIVE', 'INACTIVE', 'LOW_STOCK', 'OUT_OF_STOCK'] as const;
const PAGE_SIZE = 20;

export const load = (async ({ url, depends, parent }) => {
	await parent(); // the admin check (admin/+layout.ts) runs first
	depends('app:admin-products');
	const filters = {
		q: url.searchParams.get('q')?.trim() ?? '',
		status: STATUSES.find((status) => status === url.searchParams.get('status')),
		categoryId: url.searchParams.get('category') || undefined,
	};
	const [products, categories] = await Promise.all([
		callForPage(
			api.GET('/api/v1/admin/shop/products', {
				params: {
					query: {
						q: filters.q || undefined,
						status: filters.status,
						categoryId: filters.categoryId,
						page: intParam(url, 'page'),
						size: PAGE_SIZE,
					},
				},
			}),
		),
		callForPage(api.GET('/api/v1/admin/shop/categories')),
	]);
	return { products, categories, filters };
}) satisfies PageLoad;
