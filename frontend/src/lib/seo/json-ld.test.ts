import { describe, expect, it } from 'vitest';
import { absoluteUrl, breadcrumbs, decimalPrice, jsonLdTag } from './json-ld';

describe('jsonLdTag', () => {
	it('cannot be broken out of', () => {
		const tag = jsonLdTag({ name: 'Mug </script><script>alert(1)</script>' });
		expect(tag.match(/<\/script>/g)).toHaveLength(1);
		expect(tag).toContain('\\u003c/script>');
		expect(JSON.parse(tag.slice(tag.indexOf('>') + 1, tag.lastIndexOf('<')))).toEqual({
			name: 'Mug </script><script>alert(1)</script>',
		});
	});
});

describe('helpers', () => {
	it('builds breadcrumbs, prices and absolute URLs', () => {
		const list = breadcrumbs('https://shop.example', [{ name: 'Kitchen', path: '/categories/kitchen' }]);
		expect(list.itemListElement).toEqual([
			{ '@type': 'ListItem', position: 1, name: 'Kitchen', item: 'https://shop.example/categories/kitchen' },
		]);
		expect(decimalPrice(1250, 2)).toBe('12.50');
		expect(decimalPrice(500, 0)).toBe('500');
		expect(absoluteUrl('https://shop.example', '/api/v1/files/public/a.jpg')).toBe(
			'https://shop.example/api/v1/files/public/a.jpg',
		);
		expect(absoluteUrl('https://shop.example', 'https://cdn.example/a.jpg')).toBe('https://cdn.example/a.jpg');
		expect(absoluteUrl('https://shop.example', null)).toBeUndefined();
	});
});
