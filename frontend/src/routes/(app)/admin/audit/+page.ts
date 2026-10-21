import { api, callForPage } from '$lib/api/client';
import { intParam } from '$lib/navigation';
import type { PageLoad } from './$types';

/** Event families: the API matches `action` as a prefix (see AuditActions in the backend). */
const AUDIT_GROUPS = ['security.', 'user.', 'admin.', 'system.'] as const;
const PAGE_SIZE = 50;

export const load = (async ({ url, parent }) => {
	await parent(); // the admin check (admin/+layout.ts) runs first, so non-admins don't hit the API
	const action = AUDIT_GROUPS.find((group) => group === url.searchParams.get('action'));
	const q = url.searchParams.get('q')?.trim() ?? '';
	const events = await callForPage(
		api.GET('/api/v1/admin/audit', {
			params: { query: { action, q: q || undefined, page: intParam(url, 'page'), size: PAGE_SIZE } },
		}),
	);
	return { events, filters: { action, q } };
}) satisfies PageLoad;
