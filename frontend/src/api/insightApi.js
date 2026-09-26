// PLACEHOLDER — no backend yet. Swap back to fetch() once
// InsightController exposes GET /api/insights/hourly-focus.

const MOCK_DELAY_MS = 300;

function delay(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Fetch hourly focus aggregate data for FocusChart.
 * Generates a plausible 24h spread so the chart isn't empty during dev.
 * @param {{ from?: string, to?: string }} [range] ISO date strings (ignored in mock)
 * @returns {Promise<Array<{ hour: string, avgFocusScore: number, sessionCount: number }>>}
 */
export async function getHourlyFocus(range = {}) {
    await delay(MOCK_DELAY_MS);

    return Array.from({ length: 24 }, (_, hour) => {
        // rough bell curve peaking mid-morning + mid-afternoon, quiet overnight
        const base =
            hour >= 6 && hour <= 22
                ? 40 + 40 * Math.sin(((hour - 6) / 16) * Math.PI)
                : 5;

        return {
            hour: `${String(hour).padStart(2, "0")}:00`,
            avgFocusScore: Math.round(base + (Math.random() * 10 - 5)),
            sessionCount: hour >= 6 && hour <= 22 ? Math.floor(Math.random() * 4) : 0,
        };
    });
}
