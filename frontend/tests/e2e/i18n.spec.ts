import { expect, test } from '@playwright/test';
import { open } from './support';

test('switching to Arabic mirrors the layout', async ({ page }) => {
	await open(page, '/login');
	await expect(page.locator('html')).toHaveAttribute('dir', 'ltr');

	await page.getByRole('button', { name: 'Change language' }).click();
	await page.getByRole('menuitemradio', { name: 'العربية' }).click();

	await expect(page.locator('html')).toHaveAttribute('lang', 'ar');
	await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
	await expect(page.getByRole('button', { name: 'تسجيل الدخول' })).toBeVisible();

	// The choice is remembered (cookie), including for server-rendered pages.
	await open(page, '/');
	await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
});
