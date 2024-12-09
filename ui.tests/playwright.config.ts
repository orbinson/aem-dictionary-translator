import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
    testDir: "./tests",
    timeout: 30 * 1000,
    fullyParallel: false,
    forbidOnly: !!process.env.CI,
    retries: 1,
    workers: 1,
    reporter: "html",
    use: {
        baseURL: "http://localhost:4502",
        trace: "on-first-retry",
        httpCredentials: {
            username: "admin",
            password: "admin"
        }
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
