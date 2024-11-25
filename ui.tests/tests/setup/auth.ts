import { test as setup } from "@playwright/test";
import * as path from "path";

const authFile = path.join(__dirname, "../../playwright/.auth/user.json");

setup("AEM Login", async ({ page }) => {
    await page.goto("/");

    await page.getByPlaceholder("User name").fill("admin");
    await page.getByPlaceholder("Password", { exact: true }).fill("admin");
    await page.getByRole("button", { name: "Sign In" }).click();

    await page.waitForURL("/aem/start.html");

    await page.context().storageState({ path: authFile });
});