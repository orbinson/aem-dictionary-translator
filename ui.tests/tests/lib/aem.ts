import { authenticationHeader } from "./http";

export async function replicationQueueState(baseURL: string, httpCredentials: { username: string, password: string }) {
    try {
        const response = await fetch(`${baseURL}/etc/replication/agents.author/publish/jcr:content.queue.json`, {
            headers: authenticationHeader(httpCredentials)
        });
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error("Failed to get replication queue state: " + error);
    }
    return {};
}