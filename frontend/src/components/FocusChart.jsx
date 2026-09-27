import { useState, useEffect } from "react";
import {
    ResponsiveContainer,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
} from "recharts";
import { getHourlyFocus } from "../api/insightApi";

export default function FocusChart() {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let cancelled = false;

        async function loadData() {
            setLoading(true);
            setError(null);
            try {
                const hourlyFocus = await getHourlyFocus();
                if (!cancelled) setData(hourlyFocus);
            } catch (err) {
                if (!cancelled) setError("Couldn't load focus history.");
                console.error(err);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        loadData();
        return () => {
            cancelled = true;
        };
    }, []);

    if (loading) {
        return <p style={{ fontSize: "13px", color: "var(--text-secondary, gray)" }}>Loading focus history...</p>;
    }

    if (error) {
        return <p style={{ fontSize: "13px", color: "var(--text-danger, red)" }}>{error}</p>;
    }

    if (data.length === 0) {
        return <p style={{ fontSize: "13px", color: "var(--text-secondary, gray)" }}>No sessions yet today.</p>;
    }

    return (
        <div>
            <p style={{ fontSize: "13px", color: "var(--text-secondary, gray)", marginBottom: "10px" }}>
                Focus by hour
            </p>
            <ResponsiveContainer width="100%" height={200}>
                <BarChart data={data} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="hour" fontSize={11} tickLine={false} />
                    <YAxis fontSize={11} tickLine={false} domain={[0, 100]} />
                    <Tooltip
                        formatter={(value, name) =>
                            name === "avgFocusScore" ? [`${value}%`, "Avg focus"] : [value, "Sessions"]
                        }
                    />
                    <Bar dataKey="avgFocusScore" fill="#4ade80" radius={[4, 4, 0, 0]} />
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}
