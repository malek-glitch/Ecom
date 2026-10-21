import { error } from '@sveltejs/kit';
import { session } from '$lib/auth/session.svelte';
import { m } from '$lib/paraglide/messages';
import type { LayoutLoad } from './$types';

/** The API enforces this too; the guard just avoids showing pages that would only fail. */
export const load = (async ({ parent }) => {
	await parent();
	if (!session.isAdmin) error(403, { message: m.error_forbidden(), code: 'auth.forbidden' });
}) satisfies LayoutLoad;
