import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import type { PageLoad } from './$types';

const ROLES = ['USER', 'ADMIN'] as const;
const STATUSES = ['ACTIVE', 'DISABLED', 'UNVERIFIED'] as const;
const PAGE_SIZE = 20;

const pick = <T extends string>(allowed: readonly T[], value: string | null): T | undefined =>
	allowed.find((item) => item === value);

export const load = (async ({ url, depends, parent }) => {
	await parent(); // the admin check (admin/+layout.ts) runs first, so non-admins don't hit the API
	depends('app:admin-users');
	const filters = {
		q: url.searchParams.get('q')?.trim() ?? '',
		role: pick(ROLES, url.searchParams.get('role')),
		status: pick(STATUSES, url.searchParams.get('status')),
	};
	const users = await callForPage(
		api.GET('/api/v1/admin/users', {
			params: {
				query: {
					q: filters.q || undefined,
					role: filters.role,
					status: filters.status,
					page: intParam(url, 'page'),
					size: PAGE_SIZE,
				},
			},
		}),
	);
	return { users, filters };
}) satisfies PageLoad;
