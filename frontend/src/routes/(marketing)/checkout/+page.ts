import { redirect } from '@sveltejs/kit';
import { session } from '$lib/auth/session.svelte';
import { loginUrl } from '$lib/navigation';
import type { PageLoad } from './$types';

// Uses the cart (localStorage) and the session: browser only.
export const ssr = false;

/** Ordering needs an account (order history, emails). The login page sends people back here. */
export const load = (async ({ url }) => {
	await session.init();
	if (!session.isAuthenticated) redirect(307, loginUrl(url));
}) satisfies PageLoad;
