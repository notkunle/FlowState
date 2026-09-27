const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

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
    return res.json();
}

/**
 * Fetch hourly focus aggregate data for FocusChart.
 * @param {{ from?: string, to?: string }} [range] - yyyy-MM-dd date strings
 */
export async function getHourlyFocus({ from, to } = {}) {
    const params = new URLSearchParams();
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    const query = params.toString() ? `?${params}` : "";

    const res = await fetch(`${BASE_URL}/insights/hourly-focus${query}`);
    return handleResponse(res);
}
