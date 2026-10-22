import { isHexColor, type HeadingFont, type Radius, type StoreSettings } from './store-settings';

/** Brand colours offered as one-click swatches in Administration → Appearance. */
export const BRAND_PRESETS = [
	{ name: 'Neutral', value: '' },
	{ name: 'Blue', value: '#2563eb' },
	{ name: 'Indigo', value: '#4f46e5' },
	{ name: 'Violet', value: '#7c3aed' },
	{ name: 'Rose', value: '#e11d48' },
	{ name: 'Orange', value: '#ea580c' },
	{ name: 'Amber', value: '#d97706' },
	{ name: 'Emerald', value: '#059669' },
	{ name: 'Teal', value: '#0d9488' },
	{ name: 'Slate', value: '#334155' },
] as const;

export const RADIUS_VALUES: Record<Radius, string> = {
	none: '0rem',
	small: '0.375rem',
	medium: '0.625rem',
	large: '0.875rem',
	full: '1.25rem',
};

export const HEADING_FONT_VALUES: Record<HeadingFont, string> = {
	sans: 'var(--font-sans)',
	serif: "ui-serif, Georgia, Cambria, 'Times New Roman', 'Noto Naskh Arabic', serif",
	mono: "ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace",
};

type Rgb = [number, number, number];

const toRgb = (hex: string): Rgb => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16)) as Rgb;
const toHex = (rgb: Rgb) => `#${rgb.map((c) => Math.round(c).toString(16).padStart(2, '0')).join('')}`;

/** WCAG relative luminance, 0 (black) to 1 (white). */
export function luminance(hex: string): number {
	const [r, g, b] = toRgb(hex).map((c) => {
		const s = c / 255;
		return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
	});
	return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

export function contrast(a: string, b: string): number {
	const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
	return (hi + 0.05) / (lo + 0.05);
}

/**
 * Text colour for buttons on [background]: white while it stays clearly legible (3.4:1, above
 * the WCAG 3:1 level for bold and large text), which is what brand buttons usually use;
 * otherwise whichever of white and near-black contrasts more (yellow, amber, light colours).
 */
export function readableOn(background: string): string {
	const white = contrast(background, '#ffffff');
	if (white >= 3.4) return '#ffffff';
	return white >= contrast(background, '#171717') ? '#ffffff' : '#171717';
}

/** Mixes [hex] with white (amount > 0) or black (amount < 0). */
export function shade(hex: string, amount: number): string {
	const target = amount > 0 ? 255 : 0;
	const t = Math.abs(amount);
	return toHex(toRgb(hex).map((c) => c + (target - c) * t) as Rgb);
}

/** The variables a theme sets, for light and dark mode. Also used by the admin's live preview. */
export function themeVariables(theme: Pick<StoreSettings, 'brandColor' | 'radius' | 'headingFont'>) {
	const light: Record<string, string> = {
		'--radius': RADIUS_VALUES[theme.radius],
		'--font-heading': HEADING_FONT_VALUES[theme.headingFont],
	};
	const dark: Record<string, string> = {};
	const brand = theme.brandColor && isHexColor(theme.brandColor) ? theme.brandColor.toLowerCase() : null;
	if (brand) {
		const fg = readableOn(brand);
		Object.assign(light, {
			'--primary': brand,
			'--primary-foreground': fg,
			'--ring': shade(brand, 0.35),
			'--accent': shade(brand, 0.92),
			'--accent-foreground': shade(brand, -0.55),
			'--sidebar-primary': brand,
			'--sidebar-primary-foreground': fg,
			'--sidebar-ring': shade(brand, 0.35),
		});
		// On a dark background a dark brand colour disappears: lighten it until it stands out.
		let darkBrand = brand;
		for (let step = 0; step < 6 && contrast(darkBrand, '#252525') < 3; step++) darkBrand = shade(darkBrand, 0.2);
		const darkFg = readableOn(darkBrand);
		Object.assign(dark, {
			'--primary': darkBrand,
			'--primary-foreground': darkFg,
			'--ring': shade(darkBrand, -0.3),
			'--accent': shade(darkBrand, -0.78),
			'--accent-foreground': shade(darkBrand, 0.7),
			'--sidebar-primary': darkBrand,
			'--sidebar-primary-foreground': darkFg,
			'--sidebar-ring': shade(darkBrand, -0.3),
		});
	}
	return { light, dark };
}

const declarations = (vars: Record<string, string>) =>
	Object.entries(vars)
		.map(([name, value]) => `${name}:${value}`)
		.join(';');

/**
 * The CSS that applies the admin's theme on top of app.css. Every value comes from a fixed
 * table or a validated #rrggbb colour, so it is safe to inline in the page head.
 */
export function themeCss(theme: Pick<StoreSettings, 'brandColor' | 'radius' | 'headingFont'>): string {
	const { light, dark } = themeVariables(theme);
	// html:root / html.dark outrank app.css's :root / .dark whatever order the stylesheets load in.
	let css = `html:root{${declarations(light)}}`;
	if (Object.keys(dark).length) css += `html.dark{${declarations(dark)}}`;
	return css;
}

/** [themeCss] wrapped in a style element, built here because Svelte would treat a literal style tag in a template as its own CSS. */
export const themeStyleTag = (css: string) => `<style id="store-theme">${css}</` + 'style>';
