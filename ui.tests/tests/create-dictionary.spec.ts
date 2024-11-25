import { expect, test } from "@playwright/test";

test("Create new dictionary", async ({ page }) => {
    await page.goto("/tools/translation/dictionaries.html");

    await page.getByRole("button", { name: "Create Dictionary" }).click();
    await page.getByLabel("Basic").fill("my-dictionary");
    await page.getByLabel("Add").click();
    await page.getByLabel("Albanian (sq_al)", { exact: true }).click();
    await page.getByRole("option", { name: "English (en)" }).click();
    await page.getByLabel("Basename").fill("ui-tests");

    await Promise.all([
        page.getByRole("button", { name: "Create" }).click(),
        page.waitForURL("/tools/translation/dictionaries.html")
    ]);

    await expect(page.getByRole("gridcell", { name: "ui-tests" })).toBeVisible();
    await expect(page.getByRole("gridcell", { name: "/content/dictionaries/my-dictionary/i18n" })).toBeVisible();
});

test("Create existing dictionary gives error", async ({ page }) => {
    await page.goto("/tools/translation/dictionaries.html");

    await page.getByRole("button", { name: "Create Dictionary" }).click();
    await page.getByLabel("Basic").fill("my-dictionary");
    await page.getByLabel("Add").click();
    await page.getByLabel("Albanian (sq_al)", { exact: true }).click();
    await page.getByRole("option", { name: "English (en)" }).click();
    await page.getByLabel("Basename").fill("ui-tests");

    await page.getByRole("button", { name: "Create" }).click();

    await expect(page.getByText("Error")).toBeVisible();
});
