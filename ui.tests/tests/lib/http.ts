export function authenticationHeader(httpCredentials: { username: string; password: string }) {
    return {
        "Authorization": "Basic " + btoa(`${httpCredentials.username}:${httpCredentials.password}`)
    }
}
