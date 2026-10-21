import { describe, expect, it } from 'vitest';
import { intParam, safeRedirect, withQuery } from './navigation';

describe('safeRedirect', () => {
	it.each([
		['/notes?q=1', '/notes?q=1'],
		[null, '/dashboard'],
		['', '/dashboard'],
		['https://evil.example', '/dashboard'],
		['//evil.example', '/dashboard'],
		['/\\evil.example', '/dashboard'],
		['javascript:alert(1)', '/dashboard'],
	])('%s → %s', (target, expected) => {
		expect(safeRedirect(target)).toBe(expected);
	});
});

describe('query helpers', () => {
	it('sets and removes parameters', () => {
		const url = new URL('http://localhost/notes?q=milk&page=2');
		expect(withQuery(url, { page: null })).toBe('/notes?q=milk');
		expect(withQuery(url, { q: '', page: 0 })).toBe('/notes?page=0');
	});

	it('parses page numbers defensively', () => {
		expect(intParam(new URL('http://localhost/?page=3'), 'page')).toBe(3);
		expect(intParam(new URL('http://localhost/?page=-1'), 'page')).toBe(0);
		expect(intParam(new URL('http://localhost/?page=abc'), 'page')).toBe(0);
	});
});
