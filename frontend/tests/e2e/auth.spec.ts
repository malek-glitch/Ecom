import { expect, test } from '@playwright/test';
import { linkFromEmail, open, PASSWORD, registerAndSignIn, signIn, uniqueEmail } from './support';

test('register, confirm the email, sign in and sign out', async ({ page }) => {
	await registerAndSignIn(page, 'Ada Lovelace');
	await expect(page.getByRole('heading', { name: 'Welcome back, Ada' })).toBeVisible();

	await page.getByRole('button', { name: /Ada Lovelace/ }).click();
	await page.getByRole('menuitem', { name: 'Sign out' }).click();
	await expect(page).toHaveURL(/\/login/);

	// Signed out for real: the app sends us back to the login page.
	await page.goto('/dashboard');
	await expect(page).toHaveURL(/\/login\?redirect=%2Fdashboard/);
});

test('an unconfirmed account is asked to confirm its email', async ({ page }) => {
	const email = uniqueEmail('unconfirmed');
	await open(page, '/register');
	await page.getByLabel('Full name').fill('Not Yet');
	await page.getByLabel('Email').fill(email);
	await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
	await page.getByRole('button', { name: 'Create account' }).click();
	await expect(page.getByText('Check your inbox')).toBeVisible();

	await open(page, '/login');
	await page.getByLabel('Email').fill(email);
	await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
	await page.getByRole('button', { name: 'Sign in' }).click();
	await expect(page.getByText('Confirm your email first')).toBeVisible();
});

test('wrong password shows an error', async ({ page }) => {
	await open(page, '/login');
	await page.getByLabel('Email').fill(uniqueEmail('nobody'));
	await page.getByLabel('Password', { exact: true }).fill('not-the-password');
	await page.getByRole('button', { name: 'Sign in' }).click();
	await expect(page.getByText('Wrong email or password.')).toBeVisible();
});

test('reset a forgotten password', async ({ page }) => {
	const email = await registerAndSignIn(page);
	await page.getByRole('button', { name: /Test User/ }).click();
	await page.getByRole('menuitem', { name: 'Sign out' }).click();
	await expect(page).toHaveURL(/\/login/);

	await open(page, '/forgot-password');
	await page.getByLabel('Email').fill(email);
	await page.getByRole('button', { name: 'Send the link' }).click();
	await expect(page.getByText(`If an account exists for ${email}`)).toBeVisible();

	await open(page, await linkFromEmail(email, '/reset-password'));
	await page.getByLabel('New password').fill('a-brand-new-password');
	await page.getByLabel('Confirm the password').fill('a-brand-new-password');
	await page.getByRole('button', { name: 'Save and sign in' }).click();
	await expect(page).toHaveURL(/\/dashboard$/);

	await page.getByRole('button', { name: /Test User/ }).click();
	await page.getByRole('menuitem', { name: 'Sign out' }).click();
	await signIn(page, email, 'a-brand-new-password');
});

test('a guest is sent to the login page', async ({ page }) => {
	await page.goto('/notes');
	await expect(page).toHaveURL(/\/login\?redirect=%2Fnotes/);
});
