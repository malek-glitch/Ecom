import { expect, test } from '@playwright/test';
import { ADMIN_EMAIL, ADMIN_PASSWORD, linkFromEmail, open, registerAndSignIn, signIn, uniqueEmail } from './support';

test('invite a user, who sets a password; then disable them', async ({ browser }) => {
	const admin = await browser.newPage();
	await signIn(admin, ADMIN_EMAIL, ADMIN_PASSWORD);
	await admin.getByRole('link', { name: 'Users', exact: true }).click();
	await expect(admin.getByRole('heading', { name: 'Users', exact: true })).toBeVisible();

	const email = uniqueEmail('invited');
	await admin.getByRole('button', { name: 'Invite user' }).click();
	const dialog = admin.getByRole('dialog');
	await dialog.getByLabel('Full name').fill('Grace Hopper');
	await dialog.getByLabel('Email').fill(email);
	await dialog.getByRole('button', { name: 'Send the invitation' }).click();
	await expect(admin.getByRole('heading', { name: 'Grace Hopper' })).toBeVisible();

	// The invited person accepts in their own browser.
	const invited = await browser.newPage();
	await open(invited, await linkFromEmail(email, '/reset-password'));
	await expect(invited.getByText('Welcome! Choose a password')).toBeVisible();
	await invited.getByLabel('New password').fill('graces-own-password');
	await invited.getByLabel('Confirm the password').fill('graces-own-password');
	await invited.getByRole('button', { name: 'Save and sign in' }).click();
	await expect(invited).toHaveURL(/\/dashboard$/);

	// The admin disables the account: the user is signed out on their next request.
	await admin.reload();
	await admin.getByRole('switch', { name: 'Account enabled' }).click();
	await admin.getByRole('button', { name: 'Save' }).click();
	await expect(admin.getByText('User updated')).toBeVisible();

	await invited.getByRole('link', { name: 'Notes', exact: true }).click();
	await expect(invited).toHaveURL(/\/login/);

	await admin.getByRole('link', { name: 'Audit log' }).click();
	await expect(admin.getByText('admin.user_updated').first()).toBeVisible();
});

test('a regular user cannot open the admin pages', async ({ page }) => {
	await registerAndSignIn(page);
	await expect(page.getByRole('link', { name: 'Audit log' })).toHaveCount(0);
	await page.goto('/admin/users');
	await expect(page.getByText('Access denied')).toBeVisible();
});
