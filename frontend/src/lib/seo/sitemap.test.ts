import { describe, expect, it } from 'vitest';
import { robotsTxt, sitemapXml } from './sitemap';

describe('sitemap', () => {
	it('lists store pages with absolute, escaped URLs', () => {
		const xml = sitemapXml('https://shop.example', {
			categories: [{ slug: 'home-living', updatedAt: '2026-10-01T10:00:00Z' }],
			products: [
				{ slug: 'mug', updatedAt: '2026-10-02T10:00:00Z', imageUrl: '/api/v1/files/public/products/a&b.jpg' },
				{ slug: 'pen', updatedAt: '2026-10-03T10:00:00Z', imageUrl: null },
			],
		});
		expect(xml).toContain('<loc>https://shop.example/categories/home-living</loc><lastmod>2026-10-01</lastmod>');
		expect(xml).toContain('<image:loc>https://shop.example/api/v1/files/public/products/a&amp;b.jpg</image:loc>');
		expect(xml).toContain('<loc>https://shop.example/products/pen</loc><lastmod>2026-10-03</lastmod></url>');
	});

	it('points robots at the sitemap and away from private pages', () => {
		const robots = robotsTxt('https://shop.example');
		expect(robots).toContain('Disallow: /checkout');
		expect(robots).toContain('Sitemap: https://shop.example/sitemap.xml');
	});
});
