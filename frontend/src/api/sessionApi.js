// PLACEHOLDER — no backend yet. Swap back to fetch() once
// SessionController exposes POST /api/sessions and DELETE /api/sessions/{id}.

const MOCK_DELAY_MS = 300;

function delay(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Start a new focus session.
 * @param {{ taskLabel: string }} payload
 * @returns {Promise<{ id: string, taskLabel: string, startTime: string }>}
 */
export async function startSession({ taskLabel }) {
    await delay(MOCK_DELAY_MS);
    return {
        id: crypto.randomUUID(),
        taskLabel,
        startTime: new Date().toISOString(),
    };
}

/**
 * Stop an active focus session.
 * @param {string} sessionId
 * @returns {Promise<null>}
 */
export async function stopSession(sessionId) {
    await delay(MOCK_DELAY_MS);
    console.log(`[mock] stopped session ${sessionId}`);
    return null;
}
