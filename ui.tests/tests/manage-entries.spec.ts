import { expect, test } from "@playwright/test";
import { clearReplicationQueue, resetITContent } from "./lib/reset";

test.beforeEach(async ({ page, baseURL }) => {
    await resetITContent(baseURL);
    await clearReplicationQueue(baseURL);
    await page.goto("/tools/translation/dictionaries/message-entries.html/content/dictionaries/fruit/i18n");
});

test.afterAll(async ({ baseURL }) => {
    clearReplicationQueue(baseURL);
});

test("Dictionary menu navigation", async ({ page }) => {
    // click current directory in menu navigation
    await page.getByRole("button", { name: "/content/dictionaries/fruit/" }).click();

    // go back to overview page from menu
    await page.getByRole("option", { name: "Dictionaries" }).click();

    // make sure we are back on the overview page
    await page.waitForURL("/tools/translation/dictionaries.html");
});

test("Create new key", async ({ page }) => {
    // click the "Create Key" button in the action bar
    await page.getByRole("button", { name: "Create Key" }).click();

    // fill in the form in the modal
    await page.getByLabel("Key *").fill("another");
    await page.getByLabel("Dutch (nl)").fill("Nog een");
    await page.getByLabel("English (en)").fill("Another One");

    // submit the form and wait for the page to reload
    await Promise.all([
        page.getByRole("button", { name: "Create" }).click(),
        page.waitForURL("/tools/translation/dictionaries/message-entries.html/content/dictionaries/fruit/i18n")
    ]);

    await page.waitForEvent("domcontentloaded");

    // check if the new key is visible in the table
    await expect(page.getByRole("gridcell", { name: "another", exact: true })).toBeVisible();
    await expect(page.getByRole("gridcell", { name: "Another One", exact: true })).toBeVisible();
    await expect(page.getByRole("gridcell", { name: "Nog een", exact: true })).toBeVisible();
});