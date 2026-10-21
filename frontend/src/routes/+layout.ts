import { apiUrl } from '$lib/api/base';
import type { components } from '$lib/api/schema';
import { APP_NAME } from '$lib/config';
import type { LayoutLoad } from './$types';

export type PublicConfig = components['schemas']['PublicConfig'];

// Public pages are server-rendered by the Node build. The SPA build (`npm run build:spa`) has
// no server, so it renders everything in the browser.
export const ssr = !__SPA__;

/** Used when the API can't be reached, so public pages still render. */
const FALLBACK_CONFIG: PublicConfig = {
	appName: APP_NAME,
	registrationEnabled: true,
	requireVerifiedEmail: true,
	googleClientId: null,
	storageEnabled: false,
	locales: ['fr', 'en', 'ar'],
	defaultLocale: 'fr',
	settings: {},
};

export const load = (async ({ fetch, depends }) => {
	depends('app:config');
	try {
		const response = await fetch(apiUrl('/api/v1/public/config'));
		if (response.ok) return { config: (await response.json()) as PublicConfig };
		console.warn(`GET /api/v1/public/config answered ${response.status}; using default settings`);
	} catch (error) {
		// API down, or unreachable from the server (check API_INTERNAL_URL): render with defaults.
		console.warn('GET /api/v1/public/config failed; using default settings', error);
	}
	return { config: FALLBACK_CONFIG };
}) satisfies LayoutLoad;
