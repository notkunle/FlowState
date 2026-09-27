import { colors } from "../styles/colors";

const STATE_STYLES = {
    focus: { ...colors.focus, label: "Focused" },
    stress: { ...colors.stress, label: "Stressed" },
    neutral: { ...colors.neutral, label: "Neutral" },
};

const DEFAULT_STYLE = { ...colors.unknown, label: "Unknown" };

export default function StateBadge({ decision }) {
    if (!decision) {
        return (
            <div style={{ background: colors.surface2, border: `0.5px solid ${colors.border}`, borderRadius: "12px", padding: "1rem 1.25rem" }}>
                <p style={{ fontSize: "13px", color: colors.textSecondary, margin: 0 }}>
                    No active session — start one to see your focus state.
                </p>
            </div>
        );
    }

    const { state, reason, suggestion } = decision;
    const style = STATE_STYLES[state] ?? DEFAULT_STYLE;

    return (
        <div style={{ background: colors.surface2, border: `0.5px solid ${colors.border}`, borderRadius: "12px", padding: "1rem 1.25rem" }}>
            <p style={{ fontSize: "13px", color: colors.textSecondary, margin: "0 0 8px" }}>
                Current state
            </p>

            <span
                style={{
                    background: style.bg,
                    color: style.text,
                    fontSize: "12px",
                    padding: "4px 12px",
                    borderRadius: "999px",
                    fontWeight: 500,
                }}
            >
        {style.label}
      </span>

            {reason && (
                <p style={{ fontSize: "14px", color: colors.textPrimary, margin: "12px 0 0" }}>
                    {reason}
                </p>
            )}

            {suggestion && (
                <p style={{ fontSize: "13px", color: colors.textSecondary, margin: "6px 0 0" }}>
                    Suggestion: {suggestion}
                </p>
            )}
        </div>
    );
}
