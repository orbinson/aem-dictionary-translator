import { expect, test } from "@playwright/test";
import { clearReplicationQueue, resetITContent } from "./lib/reset";
import { replicationQueueState } from "./lib/aem";

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
    await expect(page.getByRole("dialog")).toBeVisible();

    await page.getByRole("textbox", { name: "Key *" }).focus();
    await page.getByRole("textbox", { name: "Key *" }).fill("another");
    await page.getByRole("textbox", { name: "Dutch (nl)" }).focus();
    await page.getByRole("textbox", { name: "Dutch (nl)" }).fill("Nog een");
    await page.getByRole("textbox", { name: "English (en)" }).focus();
    await page.getByRole("textbox", { name: "English (en)" }).fill("Another One");

    // submit the form and wait for the page to reload
    await page.getByRole("button", { name: "Create" }).click();

    // wait until the async replication is handled
    await page.waitForEvent("requestfinished", {
        predicate: request => request.url().endsWith("?dictionary=/content/dictionaries/fruit/i18n") && request.method() === "POST"
    });

    // check if the new key is visible in the table
    await expect(page.getByRole("gridcell", { name: "another", exact: true })).toBeVisible();
    await expect(page.getByRole("gridcell", { name: "Another One", exact: true })).toBeVisible();
    await expect(page.getByRole("gridcell", { name: "Nog een", exact: true })).toBeVisible();
});

test("Edit key", async ({ page }) => {
    const row = page.getByRole("row", { name: "apple Apple Appel" });
    await row.getByRole("checkbox").click();

    await page.getByRole("button", { name: "Edit(e)" }).click();
    await page.getByLabel("Dutch (nl)").fill("Appeltje");
    await page.getByRole("dialog").getByRole("button", { name: "Save" }).click();

    await expect(page.getByRole("row", { name: "apple Apple Appeltje" })).toBeVisible();
});

test("Publish key", async ({ page, baseURL }) => {
    // select row with apple key
    const row = page.getByRole("row", { name: "apple Apple Appel" });
    await row.getByRole("checkbox").click();

    // click the "Publish" button in the action bar
    await page.getByRole("button", { name: "Publish(p)" }).click();

    // confirm publication in dialog
    await page.getByRole("dialog").getByRole("button", { name: "Publish" }).click();

    // wait until the async replication is handled
    await page.waitForEvent("requestfinished", {
        predicate: request => request.url().endsWith("/bin/replicate")
    });

    // get items in the replication queue
    const state = await replicationQueueState(baseURL);

    // assert item is in publish queue
    expect(state.queue[0].path).toBe("/content/dictionaries/fruit/i18n/en/apple");
    expect(state.queue[0].type).toBe("Activate");
});


test("Delete key", async ({ page }) => {
    const row = page.getByRole("row", { name: "apple Apple Appel" });
    await row.getByRole("checkbox").click();

    await page.getByRole("button", { name: "Delete(backspace)" }).click();
    await page.getByRole("alertdialog").getByRole("button", { name: "Delete" }).click();

    await expect(row).toHaveCount(0);
});

test("Keys of all types exist", async ({ page }) => {
    const mixedPrimaryTypeAndMixinTypeRow = page.getByRole("row", { name: "apple Apple Appel" });
    await expect(mixedPrimaryTypeAndMixinTypeRow).toBeVisible();

    const mixinTypeRow = page.getByRole("row", { name: "banana Banana Banaan" });
    await expect(mixinTypeRow).toBeVisible();

    const primaryTypeRow = page.getByRole("row", { name: "cherry Cherry Kers" });
    await expect(primaryTypeRow).toBeVisible();
});
