import { test } from "@playwright/test";

test("Open Tools > Translation > Dictionaries", async ({ page }) => {
  await page.goto("http://localhost:4502/aem/start.html");

  await page.getByLabel("Tools").click();
  await page.getByLabel("Translation", { exact: true }).click();
  await page.getByLabel("Dictionaries").click();
});