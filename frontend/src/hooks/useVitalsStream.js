import { useState, useEffect, useRef } from "react";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

/**
 * Streams live vitals + focus-state decisions for a session over SSE.
 * @param {number | string | null | undefined} sessionId - falsy means "not streaming"
 * @returns {{
 *   vitals: { pulse: number, breathing: number, blinks: number } | null,
 *   baseline: { pulse: number, breathing: number, blinks: number } | null,
 *   decision: { state: string, reason: string, suggestion: string } | null,
 *   connected: boolean
 * }}
 */
export function useVitalsStream(sessionId) {
    const [vitals, setVitals] = useState(null);
    const [baseline, setBaseline] = useState(null);
    const [decision, setDecision] = useState(null);
    const [connected, setConnected] = useState(false);
    const esRef = useRef(null);

    useEffect(() => {
        if (!sessionId) {
            setVitals(null);
            setBaseline(null);
            setDecision(null);
            setConnected(false);
            return;
        }

        const es = new EventSource(`${BASE_URL}/sessions/${sessionId}/vitals/stream`);
        esRef.current = es;

        es.onopen = () => setConnected(true);

        es.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                setVitals(data.vitals ?? null);
                setBaseline(data.baseline ?? null);
                setDecision(data.decision ?? null);
            } catch (err) {
                console.error("Failed to parse vitals stream message", err);
            }
        };

        es.onerror = (err) => {
            console.error("Vitals stream error", err);
            setConnected(false);
            if (es.readyState === EventSource.CLOSED) {
                es.close();
            }
        };

        return () => {
            es.close();
            esRef.current = null;
            setConnected(false);
        };
    }, [sessionId]);

    return { vitals, baseline, decision, connected };
}
