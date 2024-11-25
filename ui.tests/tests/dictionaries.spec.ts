import { expect, test } from "@playwright/test";
import { CollectionPage } from "../src/page/granite/CollectionPage";

test("Dictionaries", async ({ page }) => {
    const dictionaries = new CollectionPage(page);
    await dictionaries.goto("/tools/translation/dictionaries.html");

    await expect(dictionaries.heading).toHaveText("Dictionaries");
    await expect(dictionaries.getActionBarButton("create dictionary")).toBeVisible();
    await expect(dictionaries.getTableHeaderCell(1)).toHaveText("Dictionary");
    await expect(dictionaries.getTableCell(0,1)).toHaveText("/content/dictionaries/my-dictionary/i18n")
});