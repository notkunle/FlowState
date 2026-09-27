package com.pm.flowstate.dto;

/**
 * One raw JSON line from presage-client's stdout — the actual wire format,
 * which is NOT the same shape as VitalReadingDto.
 * <p>
 * PresageReaderService converts this into VitalReadingDto so presage, fake and
 * replay all produce identical downstream data. Keeping the two apart is what
 * lets the three sources stay interchangeable: VitalReadingDto.blinks is a
 * blinks-per-minute RATE (see demo-session.jsonl and FakeVitalsGenerator),
 * while the SDK only reports blinking as a per-frame boolean event.
 * <p>
 * Example line:
 * {"recordedAt":1790482936170,"sdkTimestampUs":1790482935900123,
 *  "breathingRate":14.2,"breathingRateTimestampUs":1790482935800000,
 *  "breathingRateStable":true,"pulseRate":72.5,
 *  "pulseRateTimestampUs":1790482935900000,"pulseRateStable":true,
 *  "blinkDetected":false}
 * <p>
 * Only recordedAt and sdkTimestampUs appear on every line. Per Presage's
 * cpp/docs/metrics.md, pulse and breathing are peak/event-driven: rate_size()
 * is legitimately 0 between valid samples, so those fields are absent on most
 * lines. That is normal — not a dropped camera and not a parse failure.
 */
public record PresageLine(
        Long recordedAt,               // epoch millis, always present
        Long sdkTimestampUs,           // SmartSpectra's own clock, always present
        Double breathingRate,          // breaths per minute, often absent
        Long breathingRateTimestampUs,
        Boolean breathingRateStable,
        Double pulseRate,              // beats per minute, often absent
        Long pulseRateTimestampUs,
        Boolean pulseRateStable,
        Boolean blinkDetected          // per-frame event, NOT a rate
) {
}