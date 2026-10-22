import { APP_SLUG } from '$lib/config';

const KEY = `${APP_SLUG}.recent-searches`;
const LIMIT = 6;

/** This device's last searches, newest first (shown when the search box is focused and empty). */
export function recentSearches(): string[] {
	try {
		const saved: unknown = JSON.parse(localStorage.getItem(KEY) ?? '[]');
		return Array.isArray(saved) ? saved.filter((item): item is string => typeof item === 'string').slice(0, LIMIT) : [];
	} catch {
		return [];
	}
}

export function rememberSearch(query: string) {
	const q = query.trim().slice(0, 100);
	if (!q) return;
	try {
		const next = [q, ...recentSearches().filter((item) => item.toLowerCase() !== q.toLowerCase())].slice(0, LIMIT);
		localStorage.setItem(KEY, JSON.stringify(next));
	} catch {
		// Storage blocked: searching still works.
	}
}

export function clearRecentSearches() {
	try {
		localStorage.removeItem(KEY);
	} catch {
		// ignore
	}
}
