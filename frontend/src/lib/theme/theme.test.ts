import { describe, expect, it } from 'vitest';
import { storeSettings } from './store-settings';
import { contrast, readableOn, shade, themeCss, themeVariables } from './theme';

describe('theme', () => {
	it('picks readable text for the brand colour', () => {
		expect(readableOn('#2563eb')).toBe('#ffffff');
		expect(readableOn('#fde047')).toBe('#171717');
		expect(contrast('#000000', '#ffffff')).toBeCloseTo(21, 0);
	});

	it('lightens dark brand colours for dark mode', () => {
		const { dark } = themeVariables({ brandColor: '#1e1b4b', radius: 'medium', headingFont: 'sans' });
		expect(contrast(dark['--primary'], '#252525')).toBeGreaterThanOrEqual(3);
		expect(shade('#000000', 0.5)).toBe('#808080');
	});

	it('only ever emits known values', () => {
		const settings = storeSettings({
			'theme.brandColor': 'red;}body{display:none',
			'theme.radius': '99px',
			'theme.headingFont': 'serif',
		});
		expect(settings.brandColor).toBeNull();
		expect(settings.radius).toBe('medium');
		const css = themeCss(settings);
		expect(css).not.toContain('display:none');
		expect(css).not.toContain('--primary');
		expect(css).toContain('--radius:0.625rem');
		expect(css).toContain('ui-serif');
	});

	it('applies a valid brand colour in both modes', () => {
		const css = themeCss({ brandColor: '#059669', radius: 'large', headingFont: 'sans' });
		expect(css).toMatch(/^html:root\{.*--primary:#059669;--primary-foreground:#ffffff.*\}html\.dark\{.*--primary:/);
	});
});

describe('storeSettings', () => {
	it('drops URLs that are not http(s)', () => {
		const settings = storeSettings({
			'store.logoUrl': 'javascript:alert(1)',
			'store.heroImageUrl': 'https://cdn.example/hero.jpg',
			'store.instagramUrl': 'https://instagram.com/shop" onmouseover="x',
		});
		expect(settings.logoUrl).toBeNull();
		expect(settings.heroImageUrl).toBe('https://cdn.example/hero.jpg');
		expect(settings.social.instagram).toBeNull();
		expect(settings.showPerks).toBe(true);
	});
});
