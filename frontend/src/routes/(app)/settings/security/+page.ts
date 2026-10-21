import { api, callForPage } from '$lib/api/client';
import type { PageLoad } from './$types';

export const load = (async ({ depends }) => {
	depends('app:sessions');
	return { sessions: await callForPage(api.GET('/api/v1/me/sessions')) };
}) satisfies PageLoad;
