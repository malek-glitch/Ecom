import { expect, test } from '@playwright/test';
import { ADMIN_EMAIL, ADMIN_PASSWORD, linkFromEmail, open, registerAndSignIn, signIn } from './support';

test('an admin lists a product, a customer buys it, the admin ships it', async ({ browser }) => {
	const name = `Test Teapot ${Date.now()}`;

	// The admin creates a product.
	const admin = await browser.newPage();
	await signIn(admin, ADMIN_EMAIL, ADMIN_PASSWORD);
	await open(admin, '/admin/products/new');
	await admin.getByLabel('Name', { exact: true }).fill(name);
	await admin.getByLabel('Price (EUR)', { exact: true }).fill('20.00');
	await admin.getByLabel('Stock', { exact: true }).fill('3');
	await admin.getByRole('button', { name: 'Create' }).click();
	await expect(admin.getByText('Product created. Add a photo below.')).toBeVisible();

	// A customer finds it, fills a cart and checks out.
	const customer = await browser.newPage();
	const email = await registerAndSignIn(customer, 'Ada Lovelace');
	await open(customer, `/products?q=${encodeURIComponent(name)}`);
	await customer.getByRole('link', { name }).click();
	await customer.getByRole('button', { name: 'Increase quantity' }).click();
	await customer.getByRole('button', { name: 'Add to cart' }).click();
	await open(customer, '/cart');
	await expect(customer.getByText('€40.00').first()).toBeVisible();
	await customer.getByRole('link', { name: 'Checkout' }).click();

	await expect(customer).toHaveURL(/\/checkout$/);
	await customer.getByLabel('Address', { exact: true }).fill('12 Analytical Street');
	await customer.getByLabel('Postal code').fill('75002');
	await customer.getByLabel('City').fill('Paris');
	await customer.getByLabel('Country').fill('France');
	await customer.getByRole('button', { name: 'Place order' }).click();
	await expect(customer.getByText('Thank you for your order!')).toBeVisible();
	await expect(customer.getByText('Pending')).toBeVisible();
	const orderPath = new URL(customer.url()).pathname;
	expect(await linkFromEmail(email, '/orders/')).toBe(orderPath);

	// The admin sees it and ships it; the customer's page follows.
	await open(admin, '/admin/orders');
	await admin
		.getByRole('row', { name: /Ada Lovelace/ })
		.first()
		.getByRole('link')
		.click();
	await admin.getByRole('button', { name: 'Mark as Shipped' }).click();
	await expect(admin.getByText('Order marked as Shipped')).toBeVisible();

	await customer.reload();
	await expect(customer.getByText('Shipped', { exact: true })).toBeVisible();
	await expect(customer.getByRole('button', { name: 'Cancel order' })).toHaveCount(0);
});
