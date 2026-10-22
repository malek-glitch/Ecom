import { expect, test } from '@playwright/test';
import { ADMIN_EMAIL, ADMIN_PASSWORD, open, signIn } from './support';

test('the search box suggests products while typing, despite typos', async ({ browser }) => {
	const word = `Quillbrook${Date.now().toString(36)}`;
	const admin = await browser.newPage();
	await signIn(admin, ADMIN_EMAIL, ADMIN_PASSWORD);
	await open(admin, '/admin/products/new');
	await admin.getByLabel('Name', { exact: true }).fill(`${word} Kettle`);
	await admin.getByLabel('Price (EUR)', { exact: true }).fill('35');
	await admin.getByLabel('Stock', { exact: true }).fill('4');
	await admin.getByRole('button', { name: 'Create' }).click();
	await expect(admin.getByText('Product created. Add a photo below.')).toBeVisible();

	const shopper = await browser.newPage();
	await open(shopper, '/');
	const box = shopper.getByRole('combobox', { name: 'Search the store' });
	// A prefix with a typo in the second word.
	await box.fill(`${word.slice(0, -2)} ketle`);
	const suggestion = shopper.getByRole('option', { name: new RegExp(`${word} Kettle`) });
	await expect(suggestion).toBeVisible();
	await box.press('ArrowDown');
	await box.press('Enter');
	await expect(shopper).toHaveURL(/\/products\/quillbrook/);
	await expect(shopper.getByRole('heading', { level: 1 })).toHaveText(`${word} Kettle`);

	// The results page lists it, and the product page carries structured data for search engines.
	await open(shopper, `/products?q=${word.toLowerCase()}`);
	await expect(shopper.getByRole('link', { name: `${word} Kettle` })).toBeVisible();
	await shopper.getByRole('link', { name: `${word} Kettle` }).click();
	const jsonLd = await shopper.locator('script[type="application/ld+json"]').first().textContent();
	expect(JSON.parse(jsonLd!)[0]).toMatchObject({ '@type': 'Product', name: `${word} Kettle` });
});
