import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
    testDir: "./tests",
    timeout: 5 * 1000,
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: 1,
    reporter: "html",
    use: {
        baseURL: "http://localhost:4502",
        trace: "on-first-retry",
    },
    projects: [
        {
            name: "setup",
            testMatch: /setup\/.*\.ts/
        },
        {
            name: "chromium",
            use: {
                ...devices["Desktop Chrome"],
                storageState: "playwright/.auth/user.json"
            },
            dependencies: ["setup"]
        }
    ]
});
