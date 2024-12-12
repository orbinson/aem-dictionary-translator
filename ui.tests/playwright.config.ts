import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
    testDir: "./tests",
    timeout: 5 * 1000,
    fullyParallel: false,
    forbidOnly: !!process.env.CI,
    retries: 1,
    workers: 1,
    reporter: "html",
    use: {
        baseURL: "http://localhost:4502",
        trace: "on-first-retry",
        httpCredentials: {
            username: "test-dictionary-user",
            password: "test-dictionary-user-password"
        }
    },
    projects: [
        {
            name: "login",
            testMatch: /setup\/login\.ts/
        },
        {
            name: "preferences",
            testMatch: /setup\/preferences\.ts/
        },
        {
            name: "chromium",
            use: {
                ...devices["Desktop Chrome"],
                storageState: "playwright/.auth/user.json"
            },
            dependencies: ["login", "preferences"]
        }
    ]
});
