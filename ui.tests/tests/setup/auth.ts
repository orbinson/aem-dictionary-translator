import { test as setup } from "@playwright/test";
import * as path from "path";

const authFile = path.join(__dirname, "../../playwright/.auth/user.json");

setup("AEM Login", async ({ page, httpCredentials }) => {
    await page.goto("/");

    await page.getByPlaceholder("User name").fill(httpCredentials.username);
    await page.getByPlaceholder("Password", { exact: true }).fill(httpCredentials.password);
    await page.getByRole("button", { name: "Sign In" }).click();

    await page.waitForURL("/aem/start.html");

    await page.context().storageState({ path: authFile });
});