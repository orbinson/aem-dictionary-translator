import { expect, test } from "@playwright/test";
import { resetITContent } from "./lib/reset";

const SEARCH_FIELD = "Search keys and translations";

test.beforeEach(async ({ page, baseURL }) => {
    await resetITContent(baseURL);
    await page.goto("/tools/translation/dictionaries/message-entries.html/content/dictionaries/fruit/i18n");
});

test("Search entries by key", async ({ page }) => {
    await page.getByPlaceholder(SEARCH_FIELD).fill("berry");
    await page.getByPlaceholder(SEARCH_FIELD).press("Enter");
    await page.waitForURL(/\?q=berry$/);

    await expect(page.getByPlaceholder(SEARCH_FIELD)).toHaveValue("berry");
    await expect(page.getByRole("row", { name: "blackberry Blackberry Braam" })).toBeVisible();
    await expect(page.getByRole("row", { name: "blueberry Blueberry Blauwe bes" })).toBeVisible();
    await expect(page.getByRole("row", { name: "raspberry Raspberry Framboos" })).toBeVisible();
    await expect(page.getByRole("row", { name: "strawberry Strawberry Aardbei" })).toBeVisible();
    await expect(page.getByRole("row", { name: "apple Apple Appel" })).toHaveCount(0);
});

test("Search entries by translation", async ({ page }) => {
    // "appel" is only contained in Dutch translations
    await page.getByPlaceholder(SEARCH_FIELD).fill("appel");
    await page.getByPlaceholder(SEARCH_FIELD).press("Enter");
    await page.waitForURL(/\?q=appel$/);

    await expect(page.getByRole("row", { name: "apple Apple Appel" })).toBeVisible();
    await expect(page.getByRole("row", { name: "orange Orange Sinaasappel" })).toBeVisible();
    await expect(page.getByRole("row", { name: "pomegranate Pomegranate Granaatappel" })).toBeVisible();
    await expect(page.getByRole("row", { name: "pineapple Pineapple Ananas" })).toHaveCount(0);
});

test("Show all entries again when submitting an empty search", async ({ page }) => {
    await page.goto("/tools/translation/dictionaries/message-entries.html/content/dictionaries/fruit/i18n?q=berry");
    await expect(page.getByRole("row", { name: "apple Apple Appel" })).toHaveCount(0);

    await page.getByPlaceholder(SEARCH_FIELD).fill("");
    await page.getByPlaceholder(SEARCH_FIELD).press("Enter");
    await page.waitForURL((url) => url.search === "");

    await expect(page.getByRole("row", { name: "apple Apple Appel" })).toBeVisible();
});

test("Search automatically after typing at least 3 characters without reloading the page", async ({ page }) => {
    // lost if the page is reloaded
    await page.evaluate(() => { document.body.dataset.searchTest = "no-reload"; });

    await page.getByPlaceholder(SEARCH_FIELD).fill("ber");
    await page.waitForURL(/\?q=ber$/);

    await expect(page.getByRole("row", { name: "strawberry Strawberry Aardbei" })).toBeVisible();
    await expect(page.getByRole("row", { name: "apple Apple Appel" })).toHaveCount(0);
    await expect(page.getByPlaceholder(SEARCH_FIELD)).toBeFocused();
    await expect(page.getByPlaceholder(SEARCH_FIELD)).toHaveValue("ber");
    expect(await page.evaluate(() => document.body.dataset.searchTest)).toBe("no-reload");
});

test("Do not search automatically with less than 3 characters", async ({ page }) => {
    await page.getByPlaceholder(SEARCH_FIELD).fill("be");
    // longer than the auto search delay
    await page.waitForTimeout(1000);

    expect(new URL(page.url()).search).toBe("");
    await expect(page.getByRole("row", { name: "apple Apple Appel" })).toBeVisible();
});

test("Clear button resets the search", async ({ page }) => {
    await expect(page.getByRole("button", { name: "Clear", exact: true })).toBeDisabled();

    await page.goto("/tools/translation/dictionaries/message-entries.html/content/dictionaries/fruit/i18n?q=berry");
    await page.getByRole("button", { name: "Clear", exact: true }).click();
    await page.waitForURL((url) => url.search === "");

    await expect(page.getByPlaceholder(SEARCH_FIELD)).toHaveValue("");
    await expect(page.getByPlaceholder(SEARCH_FIELD)).toBeFocused();
    await expect(page.getByRole("button", { name: "Clear", exact: true })).toBeDisabled();
    await expect(page.getByRole("row", { name: "apple Apple Appel" })).toBeVisible();
});
