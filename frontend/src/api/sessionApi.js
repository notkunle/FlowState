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
