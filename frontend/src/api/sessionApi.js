// NO LONGER PLACEHOLDER

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api"

async function handleResponse(res) {
    if (!res.ok) {
        let message = `Request failed (${res.status})`;
        try {
            const body = await res.json();
            message = body.detail || body.message || message;
        } catch {
            // not JSON, keep default message
        }
        throw new Error(message);
    }
    if (res.status === 204) return null;
    return res.json();
}

/**
 * Start a new focus session.
 * @param {{ taskLabel: string }} payload
 * @returns {Promise<{ id: number, taskLabel: string, startedAt: string, endedAt: string|null }>}
 */
export async function startSession({ taskLabel }) {
    const params = new URLSearchParams({ taskLabel });
    const res = await fetch(`${BASE_URL}/sessions?${params}`, { method: "POST" });
    return handleResponse(res);
}

/**
 * Stop an active focus session.
 * @param {number} sessionId
 * @returns {Promise<{ id: number, taskLabel: string, startedAt: string, endedAt: string }>}
 */
export async function stopSession(sessionId) {
    const res = await fetch(`${BASE_URL}/sessions/${sessionId}/stop`, { method: "POST" });
    return handleResponse(res);
}

/**
 * Fetch a session by id.
 * @param {number} sessionId
 */
export async function getSession(sessionId) {
    const res = await fetch(`${BASE_URL}/sessions/${sessionId}`);
    return handleResponse(res);
}
