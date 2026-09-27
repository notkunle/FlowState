import { colors } from "../styles/colors";

const METRICS = [
    { key: "pulse", label: "bpm" },
    { key: "breathing", label: "br/min" },
    { key: "blinks", label: "blinks" },
];

export default function VitalsReadout({ vitals, baseline }) {
    if (!vitals) {
        return (
            <div style={{ background: colors.surface1, borderRadius: "12px", padding: "1rem" }}>
                <p style={{ fontSize: "13px", color: colors.textSecondary, margin: 0 }}>
                    Vitals will appear once a session starts.
                </p>
            </div>
        );
    }

    return (
        <div style={{ background: colors.surface1, borderRadius: "12px", padding: "1rem" }}>
            <p style={{ fontSize: "13px", color: colors.textSecondary, margin: "0 0 10px" }}>
                Vitals · live vs baseline
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "10px" }}>
                {METRICS.map(({ key, label }) => (
                    <div key={key}>
                        <p style={{ fontSize: "20px", fontWeight: 500, margin: 0, color: colors.textPrimary }}>
                            {vitals[key]}
                            <span style={{ fontSize: "12px", color: colors.textSecondary, fontWeight: 400 }}>
                {" "}{label}
              </span>
                        </p>
                        <p style={{ fontSize: "12px", color: colors.textMuted, margin: "2px 0 0" }}>
                            baseline {baseline?.[key] ?? "—"}
                        </p>
                    </div>
                ))}
            </div>
        </div>
    );
}
