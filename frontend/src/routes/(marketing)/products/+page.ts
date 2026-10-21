import { intParam } from '$lib/navigation';
import { loadOrError, storeApi } from '$lib/shop/store-api';
import type { PageLoad } from './$types';

const SORTS = ['NEWEST', 'PRICE_ASC', 'PRICE_DESC', 'NAME'] as const;
export type Sort = (typeof SORTS)[number];
const PAGE_SIZE = 24;

/** Search, category, sort and page live in the URL, so a result list can be shared or bookmarked. */
export const load = (async ({ fetch, url }) => {
	const store = storeApi(fetch, url);
	const filters = {
		q: url.searchParams.get('q')?.trim() ?? '',
		category: url.searchParams.get('category') ?? '',
		sort: SORTS.find((sort) => sort === url.searchParams.get('sort')) ?? 'NEWEST',
	};
	const [products, categories] = await Promise.all([
		loadOrError(
			store.GET('/api/v1/public/shop/products', {
				params: {
					query: {
						q: filters.q || undefined,
						category: filters.category || undefined,
						sort: filters.sort,
						page: intParam(url, 'page'),
						size: PAGE_SIZE,
					},
				},
			}),
		),
		loadOrError(store.GET('/api/v1/public/shop/categories')),
	]);
	return { products, categories, filters };
}) satisfies PageLoad;
