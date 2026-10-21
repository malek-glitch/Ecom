import { api } from '$lib/api/client';
import { session } from '$lib/auth/session.svelte';
import { getLocale, getTextDirection, locales, setLocale, type Locale } from '$lib/paraglide/runtime';

export { getLocale, locales, type Locale };

export const LOCALE_NAMES: Record<Locale, string> = { fr: 'Français', en: 'English', ar: 'العربية' };

export const textDirection = () => getTextDirection(getLocale());

/** Switches the UI language (reloads the page) and saves it on the account, so emails follow. */
export async function changeLocale(locale: Locale) {
	if (session.isAuthenticated) await api.PATCH('/api/v1/me', { body: { locale } }).catch(() => undefined);
	setLocale(locale);
}

/** After signing in, the language chosen in the UI wins over the one stored on the account. */
export function syncAccountLocale(user: { locale: string }) {
	const current = getLocale();
	if (user.locale !== current) void api.PATCH('/api/v1/me', { body: { locale: current } }).catch(() => undefined);
}
