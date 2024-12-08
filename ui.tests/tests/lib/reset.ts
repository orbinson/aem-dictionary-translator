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

export async function resetITContent() {
    const version = await readVersionFromPom();
    const filePath = path.resolve(`../it.content/target/aem-dictionary-translator.it.content-${version}.zip`);
    const fileData = await fs.readFile(filePath);
    const blob = new Blob([fileData]);

    const headers = new Headers();
    headers.append("Authorization", "Basic " + btoa("admin:admin"));

    const formData = new FormData();
    formData.append("file", blob, path.basename(filePath));
    formData.append("force", "true");
    formData.append("install", "true");

    return fetch("http://localhost:4502/crx/packmgr/service.jsp", {
        method: "POST",
        body: formData,
        headers: headers
    });
}