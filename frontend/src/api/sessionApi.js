import { request } from "./client";

/**
 * Start a new focus session. POST /api/sessions?taskLabel=...
 * Fails with 409 if another session is still running.
 * @param {{ taskLabel: string }} payload
 * @returns {Promise<{ id: number, taskLabel: string, startedAt: string, endedAt: string | null, active: boolean }>}
 */
export function startSession({ taskLabel }) {
    return request(`/sessions?taskLabel=${encodeURIComponent(taskLabel)}`, { method: "POST" });
}

/**
 * Stop a session. POST /api/sessions/{id}/stop
 * @param {number} sessionId
 */
export function stopSession(sessionId) {
    return request(`/sessions/${sessionId}/stop`, { method: "POST" });
}

/**
 * Fetch one session. GET /api/sessions/{id}
 * @param {number} sessionId
 */
export function getSession(sessionId) {
    return request(`/sessions/${sessionId}`);
}

/**
 * Fetch the session that is currently running, if any. GET /api/sessions/active
 *
 * The backend answers 204 No Content when nothing is running, which `request`
 * turns into null. Used on page load so a refresh (or a backend that was
 * started before the browser) re-attaches to the running session instead of
 * leaving the UI stuck on "No active session" while vitals stream past.
 *
 * @returns {Promise<{ id: number, taskLabel: string, startedAt: string, endedAt: string | null, active: boolean } | null>}
 */
export function getActiveSession() {
    return request(`/sessions/active`);
}