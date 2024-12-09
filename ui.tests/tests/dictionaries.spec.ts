import { expect, test } from "@playwright/test";
import { clearReplicationQueue, resetITContent } from "./lib/reset";

test.beforeEach(async ({ page, baseURL, httpCredentials }) => {
    await resetITContent(baseURL, httpCredentials);
    await clearReplicationQueue(baseURL, httpCredentials);
    await page.goto("/tools/translation/dictionaries.html");
});

test("Create new dictionary", async ({ page }) => {
    // click the "Create Dictionary" button
    await page.getByRole("button", { name: "Create Dictionary" }).click();

    // fill in the form
    await page.getByLabel("Name *").fill("new-dictionary");
    await page.getByLabel("Add").click();
    await page.getByLabel("Dutch (Belgium) (nl_be)", { exact: true }).click();
    await page.getByRole("option", { name: "English (en)" }).click();
    await page.getByLabel("Basename", { exact: true }).fill("new-dictionary-basename");

    // submit the form and wait for the page to reload
    await Promise.all([
        page.getByRole("button", { name: "Create" }).click(),
        page.waitForURL("/tools/translation/dictionaries.html")
    ]);

    // check if the new dictionary is visible in the table
    await expect(page.getByRole("row", { name: "/content/dictionaries/new-dictionary/i18n" })).toBeVisible();
    await expect(page.getByRole("gridcell", { name: "new-dictionary-basename", exact: true })).toBeVisible();
});

test("Add language to dictionary", async ({ page }) => {
    const row = page.getByRole("row", { name: "/content/dictionaries/fruit/i18n" });

    await row.getByRole("checkbox").click();

    await page.getByRole("button", { name: "Add Language(a)" }).click();

    await page.getByLabel("Dutch (Belgium) (nl_be)").click();
    await page.getByRole("option", { name: "French (fr)" }).click();

    await page.getByRole("button", { name: "Add" }).click();

    await expect(row.getByRole("gridcell", { name: "en, fr, nl", exact: true })).toHaveCount(1)
});

test("Remove language from dictionary", async ({ page }) => {
    const row = page.getByRole("row", { name: "/content/dictionaries/fruit/i18n" });

    await row.getByRole("checkbox").click();

    await page.getByRole("button", { name: "Remove Language(r)" }).click();

    await page.getByLabel("Dutch (nl)").click();
    await page.getByRole("button", { name: "Delete" }).click();

    await expect(row.getByRole("gridcell", { name: "en", exact: true })).toHaveCount(1)
});

test("Delete existing dictionary", async ({ page }) => {
    const row = page.getByRole("row", { name: "/content/dictionaries/fruit/i18n" });
    // select the existing dictionary
    await row.getByRole("checkbox").click();

    // click delete and confirm
    await page.getByRole("button", { name: "Delete(backspace)" }).click();

    await page.getByRole("button", { name: "Delete", exact: true }).click();

    // check if the dictionary is gone
    await expect(row).toHaveCount(0);
});
