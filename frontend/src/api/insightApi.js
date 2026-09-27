import { request } from "./client";

/**
 * Hourly focus aggregate for FocusChart. GET /api/insights/hourly-focus?from=&to=
 * Always 24 rows; avgFocusScore is the % of Gemini decisions in that hour that were "focus".
 * @param {{ from?: string, to?: string }} [range] ISO dates (YYYY-MM-DD); defaults to the last 31 days
 * @returns {Promise<Array<{ hour: string, avgFocusScore: number, sessionCount: number }>>}
 */
export function getHourlyFocus({ from, to } = {}) {
    const params = new URLSearchParams();
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    const query = params.toString();
    return request(`/insights/hourly-focus${query ? `?${query}` : ""}`);
}
