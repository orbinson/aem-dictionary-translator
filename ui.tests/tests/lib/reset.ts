import * as fs from "fs/promises";
import * as path from "path";

async function readVersionFromPom() {
    const pomPath = path.resolve("pom.xml");
    const pomContent = await fs.readFile(pomPath, "utf-8");
    const versionMatch = pomContent.match(/<version>([^<]+)<\/version>/);
    if (!versionMatch) {
        throw new Error("Version not found in pom.xml");
    }
    return versionMatch[1];
}

export async function clearReplicationQueue(baseURL: string, httpCredentials: { username: string, password: string }) {
    const formData = new FormData();
    formData.append("cmd", "clear");
    formData.append("agent", "publish");

    const headers = createAuthenticationHeaders(httpCredentials);

    try {
        const response = await fetch(`${baseURL}/etc/replication/agents.author/publish/jcr:content.queue.json`, {
            method: "POST",
            body: formData,
            headers: headers
        });
        await response.text();
    } catch (error) {
        throw new Error("Failed to clear replication queue: " + error);
    }
}

function createAuthenticationHeaders(httpCredentials: { username: string; password: string }) {
    const headers = new Headers();
    headers.append("Authorization", "Basic " + btoa(`${httpCredentials.username}:${httpCredentials.password}`));
    return headers;
}

export async function resetITContent(baseURL: string, httpCredentials: { username: string, password: string }) {
    const version = await readVersionFromPom();
    const filePath = path.resolve(`../it.content/target/aem-dictionary-translator.it.content-${version}.zip`);
    const fileData = await fs.readFile(filePath);
    const blob = new Blob([fileData]);

    const headers = createAuthenticationHeaders(httpCredentials);

    const formData = new FormData();
    formData.append("file", blob, path.basename(filePath));
    formData.append("force", "true");
    formData.append("install", "true");

    try {
        const response = await fetch(`${baseURL}/crx/packmgr/service.jsp`, {
            method: "POST",
            body: formData,
            headers: headers
        });
        await response.text();
    } catch (error) {
        throw new Error("Failed to reset IT content: " + error);
    }
}