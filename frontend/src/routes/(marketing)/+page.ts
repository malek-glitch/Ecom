import { loadOrError, storeApi } from '$lib/shop/store-api';
import type { PageLoad } from './$types';

export const load = (async ({ fetch, url }) => ({
	home: await loadOrError(storeApi(fetch, url).GET('/api/v1/public/shop/home')),
})) satisfies PageLoad;
