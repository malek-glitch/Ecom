/**
 * Accepts only same-site paths, so `?redirect=https://evil.example` (or `//evil.example`)
 * can't turn the login page into an open redirect.
 */
export function safeRedirect(target: string | null | undefined, fallback = '/dashboard'): string {
	if (!target || !target.startsWith('/') || target.startsWith('//') || target.startsWith('/\\')) return fallback;
	return target;
}

export function loginUrl(from: URL): string {
	return `/login?redirect=${encodeURIComponent(from.pathname + from.search)}`;
}

/** `url` with some query parameters replaced; null, undefined or '' removes one. */
export function withQuery(url: URL, changes: Record<string, string | number | null | undefined>): string {
	const next = new URL(url);
	for (const [key, value] of Object.entries(changes)) {
		if (value === null || value === undefined || value === '') next.searchParams.delete(key);
		else next.searchParams.set(key, String(value));
	}
	return next.pathname + next.search;
}

/** A non-negative integer query parameter (page numbers), with a fallback for junk. */
export function intParam(url: URL, name: string, fallback = 0): number {
	const value = Number.parseInt(url.searchParams.get(name) ?? '', 10);
	return Number.isFinite(value) && value >= 0 ? value : fallback;
}
