import { loadCatalog } from '$lib/shop/catalog';
import { loadOrError, shopSettings, storeApi } from '$lib/shop/store-api';
import type { PageLoad } from './$types';

export const load = (async ({ fetch, url, params, parent }) => {
	const { config } = await parent();
	const [category, catalog] = await Promise.all([
		loadOrError(
			storeApi(fetch, url).GET('/api/v1/public/shop/categories/{slug}', { params: { path: { slug: params.slug } } }),
		),
		loadCatalog(fetch, url, shopSettings(config.settings).currency, params.slug),
	]);
	return { category, ...catalog };
}) satisfies PageLoad;
