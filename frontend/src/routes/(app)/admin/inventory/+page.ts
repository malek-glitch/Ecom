import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import type { PageLoad } from './$types';

const STATUSES = ['LOW', 'OUT', 'IN_STOCK', 'UNTRACKED'] as const;

export const load = (async ({ url, depends, parent }) => {
	await parent();
	depends('app:inventory');
	const filters = {
		q: url.searchParams.get('q')?.trim() ?? '',
		status: STATUSES.find((status) => status === url.searchParams.get('status')),
	};
	const [summary, products, movements] = await Promise.all([
		callForPage(api.GET('/api/v1/admin/inventory/summary')),
		callForPage(
			api.GET('/api/v1/admin/inventory/products', {
				params: { query: { q: filters.q || undefined, status: filters.status, page: intParam(url, 'page'), size: 25 } },
			}),
		),
		callForPage(api.GET('/api/v1/admin/inventory/movements', { params: { query: { size: 8 } } })),
	]);
	return { summary, products, movements, filters };
}) satisfies PageLoad;
