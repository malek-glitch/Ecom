/**
 * The admin-editable look and content of the store (`theme.*`, `store.*`, `seo.*` runtime
 * settings, served in the public config), typed and sanitized. Values that don't pass the same
 * rules as the backend fall back to the defaults, so nothing odd ever reaches the page.
 */

export const RADII = ['none', 'small', 'medium', 'large', 'full'] as const;
export const HEADING_FONTS = ['sans', 'serif', 'mono'] as const;
export const IMAGE_RATIOS = ['square', 'portrait', 'landscape'] as const;

export type Radius = (typeof RADII)[number];
export type HeadingFont = (typeof HEADING_FONTS)[number];
export type ImageRatio = (typeof IMAGE_RATIOS)[number];

export interface StoreSettings {
	brandColor: string | null;
	radius: Radius;
	headingFont: HeadingFont;
	productImageRatio: ImageRatio;
	logoUrl: string | null;
	announcementBar: string;
	heroKicker: string;
	heroTitle: string;
	heroSubtitle: string;
	heroImageUrl: string | null;
	showPerks: boolean;
	footerAbout: string;
	social: { instagram: string | null; facebook: string | null; tiktok: string | null; x: string | null };
	metaDescription: string;
	shareImageUrl: string | null;
}

const HEX = /^#[0-9a-fA-F]{6}$/;
const URL_PATTERN = /^https?:\/\/[^\s"'<>]+$/;

const text = (value: unknown) => (typeof value === 'string' ? value.trim() : '');
const url = (value: unknown) => {
	const v = text(value);
	return URL_PATTERN.test(v) ? v : null;
};
const choice = <T extends string>(options: readonly T[], value: unknown, fallback: T): T =>
	options.find((option) => option === value) ?? fallback;

export const isHexColor = (value: string) => HEX.test(value);

export function storeSettings(settings: Record<string, unknown> | undefined): StoreSettings {
	const s = settings ?? {};
	const color = text(s['theme.brandColor']);
	return {
		brandColor: HEX.test(color) ? color.toLowerCase() : null,
		radius: choice(RADII, s['theme.radius'], 'medium'),
		headingFont: choice(HEADING_FONTS, s['theme.headingFont'], 'sans'),
		productImageRatio: choice(IMAGE_RATIOS, s['theme.productImageRatio'], 'square'),
		logoUrl: url(s['store.logoUrl']),
		announcementBar: text(s['store.announcementBar']),
		heroKicker: text(s['store.heroKicker']),
		heroTitle: text(s['store.heroTitle']),
		heroSubtitle: text(s['store.heroSubtitle']),
		heroImageUrl: url(s['store.heroImageUrl']),
		showPerks: s['store.showPerks'] !== false,
		footerAbout: text(s['store.footerAbout']),
		social: {
			instagram: url(s['store.instagramUrl']),
			facebook: url(s['store.facebookUrl']),
			tiktok: url(s['store.tiktokUrl']),
			x: url(s['store.xUrl']),
		},
		metaDescription: text(s['seo.metaDescription']),
		shareImageUrl: url(s['seo.shareImageUrl']),
	};
}

/** Tailwind classes for the product photo shape (written out in full so Tailwind sees them). */
export const IMAGE_RATIO_CLASS: Record<ImageRatio, string> = {
	square: 'aspect-square',
	portrait: 'aspect-[3/4]',
	landscape: 'aspect-[4/3]',
};
