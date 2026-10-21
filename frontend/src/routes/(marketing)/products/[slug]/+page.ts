import { loadOrError, storeApi } from '$lib/shop/store-api';
import type { PageLoad } from './$types';

export const load = (async ({ fetch, url, params }) => ({
	product: await loadOrError(
		storeApi(fetch, url).GET('/api/v1/public/shop/products/{slug}', { params: { path: { slug: params.slug } } }),
	),
})) satisfies PageLoad;
