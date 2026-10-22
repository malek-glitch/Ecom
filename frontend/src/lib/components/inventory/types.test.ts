import { describe, expect, it } from 'vitest';
import { stockAfter } from './types';

describe('stockAfter', () => {
	it('previews each kind of adjustment', () => {
		expect(stockAfter(10, 'RECEIVE', 5)).toBe(15);
		expect(stockAfter(10, 'REMOVE', 4)).toBe(6);
		expect(stockAfter(10, 'REMOVE', 11)).toBeNull();
		expect(stockAfter(10, 'COUNT', 7)).toBe(7);
	});
});
