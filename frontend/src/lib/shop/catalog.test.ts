import { describe, expect, it } from 'vitest';
import { isFiltered, parseFilters } from './catalog';

describe('parseFilters', () => {
	it('reads and sanitizes the URL', () => {
		const filters = parseFilters(
			new URL('https://shop.example/products?q=%20mug%20&sort=PRICE_ASC&min=10&max=abc&stock=1&page=2'),
		);
		expect(filters).toEqual({ q: 'mug', sort: 'PRICE_ASC', min: 10, max: undefined, inStock: true, page: 2 });
		expect(isFiltered(filters)).toBe(true);
	});

	it('ignores unknown values', () => {
		const filters = parseFilters(new URL('https://shop.example/products?sort=DROP&min=-5&page=-1'));
		expect(filters).toEqual({ q: '', sort: undefined, min: undefined, max: undefined, inStock: false, page: 0 });
		expect(isFiltered(filters)).toBe(false);
	});
});
