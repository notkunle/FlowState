package com.pm.flowstate.dto;

/**
 * Canonical internal reading. Both vitals sources produce this exact shape, so
 * everything downstream (VitalsService, VitalsSummaryService, GeminiService,
 * the SSE stream) has one meaning to work with:
 * <p>
 * - FakeVitalsGenerator builds it directly.
 * - PresageReaderService converts raw PresageLine objects into it, carrying
 *   the last known pulse/breathing forward across lines that have no new
 *   sample, and converting per-frame blink events into a blinks-per-minute
 *   rate.
 * <p>
 * Note this is deliberately NOT what presage-client's stdout looks like — see
 * PresageLine for that. Keeping the raw wire format separate from this is what
 * lets the fake and real sources stay interchangeable.
 * <p>
 * Any field may be null early in a session, before the SDK has produced its
 * first sample of that metric.
 */
public record VitalReadingDto(
        Double pulse,      // beats per minute
        Double breathing,  // breaths per minute
        Double blinks      // blinks per minute (a RATE, never a raw event)
) {
}