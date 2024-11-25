import { test } from '@playwright/test';

test('Open Tools > Translation > Dictionaries', async ({ page }) => {
  await page.goto('http://localhost:4502');

  await page.getByPlaceholder('User name').fill('admin');
  await page.getByPlaceholder('Password', { exact: true }).fill('admin');
  await page.getByRole('button', { name: 'Sign In' }).click();

  await page.getByLabel('Tools').click();
  await page.getByLabel('Translation', { exact: true }).click();
  await page.getByLabel('Dictionaries').click();
});