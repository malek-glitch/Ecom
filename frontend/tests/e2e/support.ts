import { expect, type Page } from '@playwright/test';

export const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025';
export const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? 'admin@liftoff.local';
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'change-me-admin-password';
export const PASSWORD = 'correct-horse-battery-staple';

export const uniqueEmail = (prefix: string) =>
	`${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}@example.com`;

/** Opens a page and waits until it is interactive (server-rendered pages hydrate first). */
export async function open(page: Page, path: string) {
	await page.goto(path);
	await expect(page.locator('html[data-hydrated]')).toBeAttached();
}

interface MailpitMessage {
	ID: string;
	Subject: string;
}

/** The first link to `path` in the newest email sent to `to`. Waits for the email to arrive. */
export async function linkFromEmail(to: string, path: string): Promise<string> {
	let link: string | undefined;
	await expect
		.poll(
			async () => {
				const search = await fetch(`${MAILPIT_URL}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`);
				const { messages } = (await search.json()) as { messages: MailpitMessage[] };
				for (const message of messages ?? []) {
					const body = (await (await fetch(`${MAILPIT_URL}/api/v1/message/${message.ID}`)).json()) as { Text: string };
					link = body.Text.match(new RegExp(`https?://\\S*${path}\\S*`))?.[0];
					if (link) return true;
				}
				return false;
			},
			{ message: `an email to ${to} with a ${path} link`, timeout: 15_000 },
		)
		.toBe(true);
	// The link points at APP_URL; keep only path + query so it works against any base URL.
	const url = new URL(link!);
	return url.pathname + url.search;
}

export async function signIn(page: Page, email: string, password: string) {
	await open(page, '/login');
	await page.getByLabel('Email').fill(email);
	await page.getByLabel('Password', { exact: true }).fill(password);
	await page.getByRole('button', { name: 'Sign in' }).click();
	await expect(page).toHaveURL(/\/dashboard$/);
}

/** Registers and confirms a new account, then signs in. */
export async function registerAndSignIn(page: Page, name = 'Test User') {
	const email = uniqueEmail('user');
	await open(page, '/register');
	await page.getByLabel('Full name').fill(name);
	await page.getByLabel('Email').fill(email);
	await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
	await page.getByRole('button', { name: 'Create account' }).click();
	await expect(page.getByText('Check your inbox')).toBeVisible();

	await open(page, await linkFromEmail(email, '/verify-email'));
	await expect(page.getByText('Your email is confirmed')).toBeVisible();

	await signIn(page, email, PASSWORD);
	return email;
}
