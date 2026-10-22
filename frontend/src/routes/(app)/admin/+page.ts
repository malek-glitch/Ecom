import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ parent }) => {
	await parent(); // the admin check (admin/+layout.ts) runs first, so non-admins don't hit the API
	const [stats, shop, searches] = await Promise.all([
		callForPage(api.GET('/api/v1/admin/stats')),
		callForPage(api.GET('/api/v1/admin/shop/stats')),
		callForPage(api.GET('/api/v1/admin/shop/search-insights')),
	]);
	return { stats, shop, searches };
}) satisfies PageLoad;
