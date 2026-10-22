/**
 * Structured data for search engines (schema.org JSON-LD), rendered as
 * `<script type="application/ld+json">`. That element is data, not code: browsers never run it,
 * so the CSP doesn't apply. `<` is escaped so text such as a product name containing
 * "</script>" can't end the element early.
 */
export type JsonLd = Record<string, unknown>;

export function jsonLdTag(data: JsonLd | JsonLd[]): string {
	const json = JSON.stringify(data)
		.replace(/</g, '\\u003c')
		.replace(/\u2028/g, '\\u2028')
		.replace(/\u2029/g, '\\u2029');
	return `<script type="application/ld+json">${json}</` + 'script>';
}

export function breadcrumbs(origin: string, items: { name: string; path: string }[]): JsonLd {
	return {
		'@context': 'https://schema.org',
		'@type': 'BreadcrumbList',
		itemListElement: items.map((item, index) => ({
			'@type': 'ListItem',
			position: index + 1,
			name: item.name,
			item: origin + item.path,
		})),
	};
}

/** "12.50" for 1250 EUR: schema.org wants a plain decimal amount. */
export function decimalPrice(minor: number, digits: number): string {
	return (minor / 10 ** digits).toFixed(digits);
}

/** Search engines want absolute image URLs; the API may return paths on this site. */
export function absoluteUrl(origin: string, url: string | null | undefined): string | undefined {
	if (!url) return undefined;
	return /^https?:\/\//.test(url) ? url : origin + (url.startsWith('/') ? url : `/${url}`);
}
