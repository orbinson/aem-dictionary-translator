import * as fs from "fs/promises";
import * as path from "path";

export async function readVersionFromPom() {
    const pomPath = path.resolve("pom.xml");
    const pomContent = await fs.readFile(pomPath, "utf-8");
    const versionMatch = pomContent.match(/<version>([^<]+)<\/version>/);
    if (!versionMatch) {
        throw new Error("Version not found in pom.xml");
    }
    return versionMatch[1];
}
