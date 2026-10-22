import { describe, expect, it } from 'vitest';
import { parseAmount, toAmountInput } from './money';

describe('parseAmount', () => {
	it('reads amounts in minor units without floating-point drift', () => {
		expect(parseAmount('12.50', 'EUR')).toBe(1250);
		expect(parseAmount('12,5', 'EUR')).toBe(1250);
		expect(parseAmount('0.29', 'EUR')).toBe(29);
		expect(parseAmount('19.99', 'USD')).toBe(1999);
		expect(parseAmount(' 7 ', 'EUR')).toBe(700);
	});

	it('follows the currency decimals', () => {
		expect(parseAmount('500', 'JPY')).toBe(500);
		expect(parseAmount('5.5', 'JPY')).toBeNull();
		expect(parseAmount('1.250', 'TND')).toBe(1250);
	});

	it('refuses junk and negative amounts', () => {
		for (const input of ['', 'abc', '-1', '1.234', '1e3', '1.2.3']) expect(parseAmount(input, 'EUR')).toBeNull();
	});
});

describe('toAmountInput', () => {
	it('formats minor units for an input field', () => {
		expect(toAmountInput(1250, 'EUR')).toBe('12.50');
		expect(toAmountInput(500, 'JPY')).toBe('500');
		expect(toAmountInput(null, 'EUR')).toBe('');
	});
});
