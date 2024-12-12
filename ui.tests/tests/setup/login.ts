import { test as setup } from "@playwright/test";
import * as path from "path";
import { urls } from "../lib/aem";

const authFile = path.join(__dirname, "../../playwright/.auth/user.json");

setup("Login to session", async ({ page, httpCredentials }) => {
    // increase timeout for initial login
    setup.setTimeout(60 * 1000);

    await page.goto("/");

    await page.getByPlaceholder("User name").fill(httpCredentials.username);
    await page.getByPlaceholder("Password", { exact: true }).fill(httpCredentials.password);
    await page.getByRole("button", { name: "Sign In" }).click();

    await page.waitForURL(urls.start);

    await page.context().storageState({ path: authFile });
});