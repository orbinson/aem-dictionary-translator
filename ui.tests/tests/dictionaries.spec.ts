import { expect, test } from "@playwright/test";
import { clearReplicationQueue, resetITContent } from "./lib/reset";
import { replicationQueueState } from "./lib/aem";

test.beforeEach(async ({ page, baseURL }) => {
    await resetITContent(baseURL);
    await clearReplicationQueue(baseURL);
    await page.goto("/tools/translation/dictionaries.html");
});

test.afterAll(async ({ baseURL }) => {
    clearReplicationQueue(baseURL);
});

test("Dictionary is visible", async ({ page }) => {
    await expect(page.getByRole("row", { name: "/content/dictionaries/fruit/i18n" })).toBeVisible();
});

test("Create new dictionary", async ({ page }) => {
    // click the "Create Dictionary" button in the action bar
    await page.getByRole("button", { name: "Create Dictionary" }).click();

    // fill in the form in the modal
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

test("Publish dictionary", async ({ page, baseURL }) => {
    // select the existing dictionary from it content
    const row = page.getByRole("row", { name: "/content/dictionaries/fruit/i18n" });
    await row.getByRole("checkbox").click();

    // click publish button in action bar
    await page.getByRole("button", { name: "Publish(p)" }).click();

    // confirm publication in modal
    await page.getByRole("button", { name: "Publish" }).click();

    // wait until the async replication is handled
    await page.waitForEvent("requestfinished", {
        predicate: request => request.url().endsWith("/bin/replicate")
    });

    // get items in the replication queue
    const state = await replicationQueueState(baseURL);

    expect(state.queue[0].path).toBe("/content/dictionaries/fruit/i18n");
    expect(state.queue[0].type).toBe("Activate");
});

test("Delete existing dictionary", async ({ page }) => {
    // select the existing dictionary from it content
    const row = page.getByRole("row", { name: "/content/dictionaries/fruit/i18n" });
    await row.getByRole("checkbox").click();

    // click delete button in action bar
    await page.getByRole("button", { name: "Delete(backspace)" }).click();

    // confirm the deletion in modal
    await page.getByRole("button", { name: "Delete", exact: true }).click();

    // make sure entry is gone in overview table
    await expect(row).toHaveCount(0);
});
