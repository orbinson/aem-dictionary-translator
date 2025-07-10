import { authenticationHeader } from "./http";
import { retry } from "./util";

export const urls = {
    replicationQueue: "/etc/replication/agents.author/publish/jcr:content.queue.json",
    currentUser: "/libs/granite/security/currentuser.json",
    packMgr: "/crx/packmgr/service.jsp",
    start: "/aem/start.html"
}

export async function replicationQueueState(baseURL: string) {
    return await retry(getReplicationQueueState, (result) => result?.queue?.length > 0, 5, 250, baseURL);
}

async function getReplicationQueueState(baseURL: string) {
    try {
        const response = await fetch(`${baseURL}${urls.replicationQueue}`, {
            headers: authenticationHeader({ username: "admin", password: "admin" })
        });
        if (response.ok) {
            // this returns at most 50 items (even if more are in the queue)
            return await response.json();
        }
    } catch (error) {
        console.error("Failed to get replication queue state: " + error);
    }
    return {};
}