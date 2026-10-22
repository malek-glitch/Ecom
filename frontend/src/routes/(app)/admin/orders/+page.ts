import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import { ORDER_STATUSES } from '$lib/shop/labels';
import type { PageLoad } from './$types';

export const load = (async ({ url, depends, parent }) => {
	await parent();
	depends('app:admin-orders');
	const filters = {
		q: url.searchParams.get('q')?.trim() ?? '',
		status: ORDER_STATUSES.find((status) => status === url.searchParams.get('status')),
	};
	const orders = await callForPage(
		api.GET('/api/v1/admin/shop/orders', {
			params: { query: { q: filters.q || undefined, status: filters.status, page: intParam(url, 'page'), size: 20 } },
		}),
	);
	return { orders, filters };
}) satisfies PageLoad;
