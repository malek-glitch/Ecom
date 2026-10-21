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
