import { getLocale } from '$lib/paraglide/runtime';

// Arabic with Latin digits, the common choice in the Maghreb; use 'ar' for Arabic-Indic digits.
const INTL_LOCALES: Record<string, string> = { fr: 'fr-FR', en: 'en-GB', ar: 'ar-u-nu-latn' };

const intlLocale = () => INTL_LOCALES[getLocale()] ?? getLocale();

export function formatDate(value: string | Date, options: Intl.DateTimeFormatOptions = { dateStyle: 'medium' }) {
	return new Intl.DateTimeFormat(intlLocale(), options).format(new Date(value));
}

export function formatDateTime(value: string | Date) {
	return formatDate(value, { dateStyle: 'medium', timeStyle: 'short' });
}

export function formatNumber(value: number) {
	return new Intl.NumberFormat(intlLocale()).format(value);
}

/** How many decimals a currency uses: 2 for EUR, 0 for JPY, 3 for TND. */
export function currencyDigits(currency: string): number {
	try {
		return new Intl.NumberFormat('en', { style: 'currency', currency }).resolvedOptions().maximumFractionDigits ?? 2;
	} catch {
		return 2;
	}
}

/**
 * Money from the API, which counts in minor units (cents): `formatMoney(1250, 'EUR')` → "12,50 €".
 * An unknown currency code falls back to "12.50 XYZ" instead of throwing.
 */
export function formatMoney(minor: number, currency: string) {
	const digits = currencyDigits(currency);
	const amount = minor / 10 ** digits;
	try {
		return new Intl.NumberFormat(intlLocale(), { style: 'currency', currency }).format(amount);
	} catch {
		return `${amount.toFixed(digits)} ${currency}`;
	}
}

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
	['year', 31_536_000],
	['month', 2_592_000],
	['week', 604_800],
	['day', 86_400],
	['hour', 3_600],
	['minute', 60],
	['second', 1],
];

/** "3 hours ago", "il y a 3 heures", "قبل 3 ساعات". */
export function formatRelative(value: string | Date, now = Date.now()) {
	const seconds = (new Date(value).getTime() - now) / 1000;
	const formatter = new Intl.RelativeTimeFormat(intlLocale(), { numeric: 'auto' });
	for (const [unit, size] of UNITS) {
		if (Math.abs(seconds) >= size || unit === 'second') return formatter.format(Math.round(seconds / size), unit);
	}
	return formatter.format(0, 'second');
}
