import { useState, useEffect, useRef } from "react";

// PLACEHOLDER — no backend yet. Swap the mock interval below for a real
// EventSource against VitalsController's SSE stream once it exists:
//
//   const es = new EventSource(`${BASE_URL}/sessions/${sessionId}/vitals/stream`);
//   es.onmessage = (event) => setData(JSON.parse(event.data));
//   es.onerror = () => setConnected(false);
//   return () => es.close();

const STATES = [
    {
        state: "focused",
        reason: "Steady pulse, low blink rate.",
        suggestion: "Keep going, no break needed yet.",
    },
    {
        state: "distracted",
        reason: "Elevated blink rate, irregular breathing.",
        suggestion: "Consider a short reset before continuing.",
    },
    {
        state: "fatigued",
        reason: "Pulse and breathing both trending down.",
        suggestion: "A 5-minute break would help right now.",
    },
];

const BASELINE = { pulse: 71, breathing: 15, blinks: 17 };

function randomAround(value, spread) {
    return Math.round(value + (Math.random() * spread * 2 - spread));
}

/**
 * Streams live vitals + focus-state decisions for a session.
 * @param {string | null} sessionId - null/undefined means "not streaming"
 * @returns {{
 *   vitals: { pulse: number, breathing: number, blinks: number } | null,
 *   baseline: { pulse: number, breathing: number, blinks: number },
 *   decision: { state: string, reason: string, suggestion: string } | null,
 *   connected: boolean
 * }}
 */
export function useVitalsStream(sessionId) {
    const [vitals, setVitals] = useState(null);
    const [decision, setDecision] = useState(null);
    const [connected, setConnected] = useState(false);
    const intervalRef = useRef(null);

    useEffect(() => {
        if (!sessionId) {
            setVitals(null);
            setDecision(null);
            setConnected(false);
            return;
        }

        setConnected(true);

        intervalRef.current = setInterval(() => {
            setVitals({
                pulse: randomAround(BASELINE.pulse, 6),
                breathing: randomAround(BASELINE.breathing, 2),
                blinks: randomAround(BASELINE.blinks, 8),
            });
            setDecision(STATES[Math.floor(Math.random() * STATES.length)]);
        }, 2000);

        return () => {
            clearInterval(intervalRef.current);
            setConnected(false);
        };
    }, [sessionId]);

    return { vitals, baseline: BASELINE, decision, connected };
}
