import { currencyDigits } from '$lib/i18n/format';

/** Minor units → what an admin types in a price field: 1250 EUR → "12.50", 500 JPY → "500". */
export function toAmountInput(minor: number | null | undefined, currency: string): string {
	if (minor === null || minor === undefined) return '';
	const digits = currencyDigits(currency);
	return (minor / 10 ** digits).toFixed(digits);
}

/**
 * A typed amount → minor units, or null when it isn't a valid non-negative amount.
 * Accepts a comma as the decimal separator ("12,5" → 1250 for EUR).
 */
export function parseAmount(input: string, currency: string): number | null {
	const digits = currencyDigits(currency);
	const value = input.trim().replace(',', '.');
	const pattern = digits > 0 ? new RegExp(`^\\d{1,10}(\\.\\d{1,${digits}})?$`) : /^\d{1,12}$/;
	if (!pattern.test(value)) return null;
	const [whole, fraction = ''] = value.split('.');
	return Number(whole) * 10 ** digits + Number(fraction.padEnd(digits, '0') || 0);
}
