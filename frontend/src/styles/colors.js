// Shared earthy/green palette. State keys (focus/stress/neutral) match
// GeminiService.STATES on the backend exactly — don't rename without
// updating both sides.

export const colors = {
    focus: { bg: "#e3e8d8", text: "#4a5d3a" },    // sage green
    stress: { bg: "#e8dcd2", text: "#8a5a3f" },    // soft terracotta
    neutral: { bg: "#f0e6d2", text: "#8a6d3a" },   // muted ochre
    unknown: { bg: "#e5e1d8", text: "#6b6455" },   // warm gray fallback

    surface1: "#f4f1e8",
    surface2: "#ece7d8",
    border: "#ddd6c4",
    textPrimary: "#3d3a2f",
    textSecondary: "#7a7361",
    textMuted: "#a39d8a",
    accent: "#7c8b5f",
    accentLight: "#a3b085",
};
