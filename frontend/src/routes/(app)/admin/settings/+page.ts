import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ depends, parent }) => {
	await parent(); // the admin check (admin/+layout.ts) runs first, so non-admins don't hit the API
	depends('app:admin-settings');
	return { settings: await callForPage(api.GET('/api/v1/admin/settings')) };
}) satisfies PageLoad;
