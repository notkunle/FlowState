package com.pm.flowstate.dto;

// last 30s of vitals compared with the user's own baseline (first few minutes of the session)
public record VitalsSummary(
        Long sessionId,
        String taskLabel,
        long minutesIntoSession,
        boolean baselineReady,     // false while still inside the baseline window
        VitalReadingDto baseline,  // averages over the baseline window
        VitalReadingDto recent,    // averages over the last 30s
        VitalReadingDto changePct  // (recent - baseline) / baseline * 100, null until baseline is ready
) {
}
