import { browser } from '$app/environment';
import { redirect } from '@sveltejs/kit';
import { session } from '$lib/auth/session.svelte';
import { safeRedirect } from '$lib/navigation';
import type { LayoutLoad } from './$types';

/** Pages a signed-in user has no reason to see. Reset and verify links still work while signed in. */
const GUEST_ONLY = new Set(['/login', '/register', '/forgot-password']);

export const load = (async ({ url }) => {
	// The server can't see tokens (they're in localStorage), so this runs in the browser.
	if (!browser || !GUEST_ONLY.has(url.pathname)) return;
	await session.init();
	if (session.user) redirect(307, safeRedirect(url.searchParams.get('redirect')));
}) satisfies LayoutLoad;
