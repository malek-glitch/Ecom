import { error } from '@sveltejs/kit';
import { apiUrl } from '$lib/api/base';
import type { components } from '$lib/api/schema';
import { sitemapXml } from '$lib/seo/sitemap';
import type { RequestHandler } from './$types';

/** Built from the live catalog (hooks.server.ts routes the API call to the backend). Cached for an hour. */
export const GET: RequestHandler = async ({ fetch, url }) => {
	const response = await fetch(apiUrl('/api/v1/public/shop/sitemap'));
	if (!response.ok) error(503, 'Catalog unavailable');
	const entries = (await response.json()) as components['schemas']['SitemapEntries'];
	return new Response(sitemapXml(url.origin, entries), {
		headers: { 'Content-Type': 'application/xml; charset=utf-8', 'Cache-Control': 'public, max-age=3600' },
	});
};
