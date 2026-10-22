import { robotsTxt } from '$lib/seo/sitemap';
import type { RequestHandler } from './$types';

/** Generated so the Sitemap line carries this site's own address. */
export const GET: RequestHandler = ({ url }) =>
	new Response(robotsTxt(url.origin), {
		headers: { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'public, max-age=86400' },
	});
