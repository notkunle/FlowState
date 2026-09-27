// Backend base URL. Override with VITE_API_URL in frontend/.env.local if the backend isn't on 8086.
export const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8086/api";

/**
 * fetch() wrapper: returns parsed JSON, throws with the backend's message on errors.
 * The backend always answers errors as {"status": 409, "error": "A session is already running"}.
 */
export async function request(path, options = {}) {
    const res = await fetch(`${BASE_URL}${path}`, options);
    const body = await res.json().catch(() => null);
    if (!res.ok) {
        throw new Error(body?.error ?? `Request failed (${res.status})`);
    }
    return body;
}
