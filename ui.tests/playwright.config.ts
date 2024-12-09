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
        trace: "on",
        httpCredentials: {
            username: "admin",
            password: "admin",
            send: "unauthorized"
        }
    },
    projects: [
        {
            name: "chromium",
            use: {
                ...devices["Desktop Chrome"],
                storageState: "playwright/.auth/user.json"
            }
        }
    ]
});
