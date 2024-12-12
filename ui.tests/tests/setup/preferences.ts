import { test as setup } from "@playwright/test";
import { authenticationHeader } from "../lib/http";
import { urls } from "../lib/aem";

setup("Set user preferences", async ({ baseURL, httpCredentials }) => {
    const user = await fetch(`${baseURL}${urls.currentUser}`, {
        headers: authenticationHeader(httpCredentials)
    })
    const userinfo = await user.json();

    if (userinfo?.home) {
        const formData = new FormData();
        formData.append("granite.shell.showonboarding620", "false");

        const preferences = await fetch(`${baseURL}${userinfo.home}/preferences`, {
            method: "POST",
            body: formData,
            headers: authenticationHeader(httpCredentials)
        });

        if (!preferences.ok) {
            throw new Error(`Failed to set user preferences: ${preferences.statusText}`);
        }
    } else {
        throw new Error("Failed to retrieve user information");
    }
});