import { expect, test } from "@playwright/test";
import { resetITContent } from "./lib/reset";

test.beforeEach(async ({ page }) => {
    await resetITContent();
    await page.goto("/tools/translation/dictionaries.html");
});

test("Create new dictionary", async ({ page }) => {
    // click the "Create Dictionary" button
    await page.getByRole("button", { name: "Create Dictionary" }).click();

    // fill in the form
    await page.getByLabel("Basic").fill("new-dictionary");
    await page.getByRole("button", { name: "Add" }).click();
    await page.getByLabel("Dutch (Belgium) (nl_be)", { exact: true }).click();
    await page.getByRole("option", { name: "English (en)" }).click();
    await page.getByLabel("Basename").fill("ui-tests");

    // submit the form and wait for the page to reload
    await Promise.all([
        page.getByRole("button", { name: "Create" }).click(),
        page.waitForURL("/tools/translation/dictionaries.html")
    ]);

    // check if the new dictionary is visible in the table
    await expect(page.getByRole("gridcell", { name: "ui-tests", exact: true })).toBeVisible();
    await expect(page.getByRole("gridcell", { name: "/content/dictionaries/new-dictionary/i18n", exact: true })).toBeVisible();
});

test("Delete existing dictionary", async ({ page }) => {
    // select the existing dictionary
    await page.getByRole("gridcell", { name: "Select /content/dictionaries/fruit/i18n" }).click();

    // click delete and confirm
    await page.getByRole("button", { name: "Delete(backspace)" })
    await page.getByRole("button", { name: "Delete" }).click();

    // check if the dictionary is gone
    await expect(page.getByRole("gridcell", { name: "/content/dictionaries/fruit" })).toHaveCount(0)
});

test("Dictionaries", async ({ page }) => {
    // add a new language to the dictionary
    await page.getByLabel("/content/dictionaries/new-dictionary").getByLabel("").check();
    await page.getByRole("button", { name: "Create Language(c)" }).click();
    await page.getByLabel("Albanian (Albania) (sq_al)", { exact: true }).click();
    await page.getByRole("option", { name: "Arabic (ar)" }).click();

    await page.getByRole("button", { name: "Create" }).click();
    expect(page.getByRole("gridcell", { name: "en,ar", exact: true })).toBeVisible();

    // delete the new language
    await page.getByLabel("/content/dictionaries/new-dictionary").getByLabel("").check();
    await page.getByRole("button", { name: "Delete Language" }).click();
    await page.getByLabel("Arabic (ar)").click();
    await page.getByRole("button", { name: "Delete" }).click();

    // publish the dictionary
    await page.getByLabel("/content/dictionaries/new-dictionary").getByLabel("").check();
    await page.getByRole("button", { name: "Publish(p)" }).click();
    await page.getByRole("button", { name: "publish" }).click();
    // expect(page.locator("coral-toast")).toHaveText("The item has been published");

    // publish the dictionary to preview
    await page.getByLabel("/content/dictionaries/new-dictionary").getByLabel("").check();
    await page.getByRole("button", { name: "Publish to Preview" }).click();
    await page.getByRole("button", { name: "publish" }).click();
    expect(page.locator("coral-toast")).toHaveText("The item has been published");
});