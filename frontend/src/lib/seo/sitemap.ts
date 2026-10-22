import type { components } from '$lib/api/schema';

type Entries = components['schemas']['SitemapEntries'];

const escapeXml = (value: string) =>
	value
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')
		.replace(/"/g, '&quot;')
		.replace(/'/g, '&apos;');

const date = (iso: string) => iso.slice(0, 10);

/** sitemap.xml for the public store: home, catalog, categories and products (with their photo). */
export function sitemapXml(origin: string, entries: Entries): string {
	const absolute = (url: string) => (/^https?:\/\//.test(url) ? url : origin + url);
	const urls = [
		`<url><loc>${escapeXml(origin)}/</loc><changefreq>daily</changefreq><priority>1.0</priority></url>`,
		`<url><loc>${escapeXml(origin)}/products</loc><changefreq>daily</changefreq><priority>0.8</priority></url>`,
		...entries.categories.map(
			(category) =>
				`<url><loc>${escapeXml(`${origin}/categories/${category.slug}`)}</loc><lastmod>${date(category.updatedAt)}</lastmod><priority>0.7</priority></url>`,
		),
		...entries.products.map((product) => {
			const image = product.imageUrl
				? `<image:image><image:loc>${escapeXml(absolute(product.imageUrl))}</image:loc></image:image>`
				: '';
			return `<url><loc>${escapeXml(`${origin}/products/${product.slug}`)}</loc><lastmod>${date(product.updatedAt)}</lastmod>${image}</url>`;
		}),
	];
	return (
		'<?xml version="1.0" encoding="UTF-8"?>\n' +
		'<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">\n' +
		urls.join('\n') +
		'\n</urlset>\n'
	);
}

/** Pages search engines shouldn't crawl: private, per-visitor or forms. */
export const DISALLOWED = [
	'/admin',
	'/dashboard',
	'/orders',
	'/settings',
	'/notes', // [example:notes]
	'/cart',
	'/checkout',
	'/login',
	'/register',
	'/forgot-password',
	'/reset-password',
	'/verify-email',
	'/api/',
];

export function robotsTxt(origin: string): string {
	return [
		'User-agent: *',
		'Allow: /',
		...DISALLOWED.map((path) => `Disallow: ${path}`),
		'',
		`Sitemap: ${origin}/sitemap.xml`,
		'',
	].join('\n');
}
