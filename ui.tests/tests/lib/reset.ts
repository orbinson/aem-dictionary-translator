import * as fs from "fs/promises";
import * as path from "path";
import { authenticationHeader } from "./http";
import { readVersionFromPom } from "./maven";
import { urls } from "./aem";

export async function clearReplicationQueue(baseURL: string, httpCredentials: { username: string, password: string }) {
    const formData = new FormData();
    formData.append("cmd", "clear");
    formData.append("agent", "publish");

    try {
        const response = await fetch(`${baseURL}${urls.replicationQueue}`, {
            method: "POST",
            body: formData,
            headers: authenticationHeader(httpCredentials)
        });
        await response.text();
    } catch (error) {
        throw new Error("Failed to clear replication queue: " + error);
    }
}

export async function resetITContent(baseURL: string, httpCredentials: { username: string, password: string }) {
    const version = await readVersionFromPom();
    const filePath = path.resolve(`target/dependency/aem-dictionary-translator.it.content-${version}.zip`);
    const fileData = await fs.readFile(filePath);
    const blob = new Blob([fileData]);

    const formData = new FormData();
    formData.append("file", blob, path.basename(filePath));
    formData.append("force", "true");
    formData.append("install", "true");

    try {
        const response = await fetch(`${baseURL}${urls.packMgr}`, {
            method: "POST",
            body: formData,
            headers: authenticationHeader(httpCredentials)
        });
        await response.text();
    } catch (error) {
        throw new Error("Failed to reset IT content: " + error);
    }
}