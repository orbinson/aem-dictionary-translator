import { authenticationHeader } from "./http";

export const urls = {
    replicationQueue: "/etc/replication/agents.author/publish/jcr:content.queue.json",
    currentUser: "/libs/granite/security/currentuser.json",
    packMgr: "/crx/packmgr/service.jsp",
    start: "/aem/start.html"
}
export async function replicationQueueState(baseURL: string) {
    try {
        const response = await fetch(`${baseURL}${urls.replicationQueue}`, {
            headers: authenticationHeader({ username: "admin", password: "admin" })
        });
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error("Failed to get replication queue state: " + error);
    }
    return {};
}