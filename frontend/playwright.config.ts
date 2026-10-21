import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests run against a running stack, by default the Docker one:
 *
 *   docker compose --profile app up -d --build   (from the repository root)
 *   npm run test:e2e
 *
 * They read emails from Mailpit's API, so the backend must send mail to Mailpit (the default),
 * and they sign in more often than the login rate limit allows: start the stack with
 * RATE_LIMIT_ENABLED=false.
 */
export default defineConfig({
	testDir: 'tests/e2e',
	// The tests share one mailbox and one admin account: run them one at a time.
	workers: 1,
	fullyParallel: false,
	forbidOnly: !!process.env.CI,
	reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
	use: {
		baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8000',
		locale: 'en-GB',
		trace: 'retain-on-failure',
		screenshot: 'only-on-failure',
		// Optional: a Chromium that's already installed, instead of `npx playwright install`.
		launchOptions: process.env.E2E_CHROMIUM_PATH ? { executablePath: process.env.E2E_CHROMIUM_PATH } : {},
	},
	projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
