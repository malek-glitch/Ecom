import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ params, depends, parent }) => {
	await parent(); // the admin check (admin/+layout.ts) runs first, so non-admins don't hit the API
	depends('app:admin-user');
	return { user: await callForPage(api.GET('/api/v1/admin/users/{id}', { params: { path: { id: params.id } } })) };
}) satisfies PageLoad;
