import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ parent }) => {
	await parent();
	return { categories: await callForPage(api.GET('/api/v1/admin/shop/categories')) };
}) satisfies PageLoad;
