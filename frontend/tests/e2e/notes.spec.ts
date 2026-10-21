import { expect, test } from '@playwright/test';
import { registerAndSignIn } from './support';

test('create, search, edit and delete a note', async ({ page }) => {
	await registerAndSignIn(page);
	await page.getByRole('link', { name: 'Notes', exact: true }).click();
	await expect(page.getByText('No notes yet')).toBeVisible();

	await page.getByRole('button', { name: 'New note' }).first().click();
	const dialog = page.getByRole('dialog');
	await dialog.getByLabel('Title').fill('Groceries');
	await dialog.getByLabel('Content').fill('Milk, bread, 50% off coffee');
	await dialog.getByRole('button', { name: 'Create' }).click();
	await expect(page.getByRole('link', { name: /Groceries/ })).toBeVisible();

	await page.getByRole('searchbox').fill('coffee');
	await expect(page).toHaveURL(/q=coffee/);
	await expect(page.getByRole('link', { name: /Groceries/ })).toBeVisible();
	await page.getByRole('searchbox').fill('nothing-matches');
	await expect(page.getByText('No notes match your search.')).toBeVisible();
	await page.getByRole('searchbox').fill('');

	await page.getByRole('link', { name: /Groceries/ }).click();
	await page.getByLabel('Title').fill('Groceries for Sunday');
	await page.getByRole('button', { name: 'Save' }).click();
	await expect(page.getByText('Note saved')).toBeVisible();

	await page.getByRole('button', { name: 'Delete' }).click();
	await page.getByRole('alertdialog').getByRole('button', { name: 'Delete' }).click();
	await expect(page).toHaveURL(/\/notes$/);
	await expect(page.getByText('No notes yet')).toBeVisible();
});
