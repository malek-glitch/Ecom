import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import type { PageLoad } from './$types';

export const load = (async ({ url, depends }) => {
	depends('app:orders');
	const orders = await callForPage(
		api.GET('/api/v1/orders', { params: { query: { page: intParam(url, 'page'), size: 20 } } }),
	);
	return { orders };
}) satisfies PageLoad;
