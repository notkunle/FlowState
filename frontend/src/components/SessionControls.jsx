import { useState } from "react";
import { useSession } from "../context/SessionContext";
import { startSession, stopSession } from "../api/sessionApi";
import { colors } from "../styles/colors";

export default function SessionControls() {
    const { session, setSession } = useSession();
    const [taskLabel, setTaskLabel] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const isActive = Boolean(session);

    async function handleStart() {
        if (!taskLabel.trim()) {
            setError("Enter a task label first.");
            return;
        }
        setError(null);
        setLoading(true);
        try {
            const newSession = await startSession({ taskLabel: taskLabel.trim() });
            setSession(newSession);
        } catch (err) {
            setError(err.message || "Couldn't start the session. Try again.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    async function handleStop() {
        if (!session) return;
        setLoading(true);
        try {
            await stopSession(session.id);
            setSession(null);
            setTaskLabel("");
        } catch (err) {
            setError(err.message || "Couldn't stop the session. Try again.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                <input
                    type="text"
                    value={taskLabel}
                    onChange={(e) => setTaskLabel(e.target.value)}
                    placeholder="What are you working on?"
                    disabled={isActive || loading}
                />
                {isActive ? (
                    <button onClick={handleStop} disabled={loading} className="stop-btn">
                        {loading ? "Stopping..." : "⏸ Stop session"}
                    </button>
                ) : (
                    <button onClick={handleStart} disabled={loading} className="start-btn">
                        {loading ? "Starting..." : "🌱 Start session"}
                    </button>
                )}
            </div>

            {error && (
                <p style={{ color: "#a3452f", fontSize: "13px", marginTop: "6px" }}>
                    {error}
                </p>
            )}

            {isActive && (
                <p style={{ fontSize: "13px", color: colors.textSecondary, marginTop: "6px" }}>
                    Session running: {session.taskLabel}
                </p>
            )}
        </div>
    );
}
