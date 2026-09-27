import { useState, useEffect } from "react";
import { BASE_URL } from "../api/client";

function round1(value) {
    return value == null ? null : Math.round(value * 10) / 10;
}

function roundVitals(v) {
    return v ? { pulse: round1(v.pulse), breathing: round1(v.breathing), blinks: round1(v.blinks) } : null;
}

/**
 * Streams live vitals + Gemini decisions for a session from
 * GET /api/sessions/{id}/vitals/stream (Server-Sent Events).
 * Each message is { vitals, baseline, decision }; baseline and decision stay null
 * until the backend has them (baseline after ~3 min, first decision shortly after).
 * @param {number | null} sessionId - null/undefined means "not streaming"
 * @returns {{
 *   vitals: { pulse: number, breathing: number, blinks: number } | null,
 *   baseline: { pulse: number, breathing: number, blinks: number } | null,
 *   decision: { state: "focus" | "stress" | "neutral", reason: string, suggestion: string } | null,
 *   connected: boolean
 * }}
 */
export function useVitalsStream(sessionId) {
    const [vitals, setVitals] = useState(null);
    const [baseline, setBaseline] = useState(null);
    const [decision, setDecision] = useState(null);
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        setVitals(null);
        setBaseline(null);
        setDecision(null);
        setConnected(false);
        if (!sessionId) return;

        const es = new EventSource(`${BASE_URL}/sessions/${sessionId}/vitals/stream`);
        es.onopen = () => setConnected(true);
        es.onmessage = (event) => {
            const data = JSON.parse(event.data);
            setVitals(roundVitals(data.vitals));
            setBaseline(roundVitals(data.baseline));
            setDecision(data.decision);
        };
        es.onerror = () => setConnected(false); // EventSource retries on its own

        return () => es.close();
    }, [sessionId]);

    return { vitals, baseline, decision, connected };
}
