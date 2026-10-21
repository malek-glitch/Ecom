import { error, redirect } from '@sveltejs/kit';
import { session } from '$lib/auth/session.svelte';
import { loginUrl } from '$lib/navigation';
import { m } from '$lib/paraglide/messages';
import type { LayoutLoad } from './$types';

// Signed-in pages render in the browser only: the tokens live there, not in a cookie.
export const ssr = false;

export const load = (async ({ url }) => {
	await session.init();
	if (session.isAuthenticated && !session.user) await session.reloadUser();
	if (!session.isAuthenticated) redirect(307, loginUrl(url));
	// Still signed in but the API didn't answer: an error page beats bouncing through /login.
	if (!session.user) error(503, { message: m.error_network(), code: 'network' });
}) satisfies LayoutLoad;
