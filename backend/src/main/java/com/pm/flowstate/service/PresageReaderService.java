package com.pm.flowstate.service;

import com.pm.flowstate.dto.PresageLine;
import com.pm.flowstate.dto.VitalReadingDto;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

// runs presage-client (webcam) and converts its JSON lines into readings
// (flowstate.vitals.source=presage)
//
// The conversion is the point of this class. presage-client's stdout does NOT
// look like VitalReadingDto (see PresageLine), and three things have to happen
// before a line is usable:
//
// 1. Field mapping — pulseRate/breathingRate/blinkDetected, not pulse/breathing/blinks.
// 2. Carry-forward — pulse and breathing are event-driven and absent on most
//    lines, so the last known value is held instead of overwriting it with null.
// 3. Blink events -> blink rate — the SDK reports blinking as a per-frame
//    boolean; blinks-per-minute is what fake/replay produce and what Flow State
//    actually reasons about (a lowered blink rate is the flow signal).
//
// Plus throttling: presage-client emits ~30 lines/sec, so readings are saved at
// most once per emit-interval-ms to match the 1 Hz cadence of the other sources.
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "flowstate.vitals.source", havingValue = "presage")
public class PresageReaderService {

    /** Rolling window used to turn blink events into blinks per minute. */
    private static final long BLINK_WINDOW_MS = 60_000L;

    private final VitalsService vitalsService;
    private final JsonMapper jsonMapper;

    @Value("${flowstate.presage.executable}")
    private String executable;

    @Value("${flowstate.presage.api-key}")
    private String apiKey;

    /**
     * Folder the SmartSpectra Windows ZIP was extracted to (e.g. C:/SmartSpectra).
     * Its bin/ is prepended to the child's PATH so smartspectra.dll,
     * opencv_world*.dll and vulkan-1.dll resolve. Leave blank if that bin/ is
     * already on the PATH of whatever shell starts the backend.
     */
    @Value("${flowstate.presage.sdk-path:}")
    private String sdkPath;

    @Value("${flowstate.presage.emit-interval-ms:1000}")
    private long emitIntervalMs;

    private Process process;

    // --- reader state, touched only by the presage-reader thread ---
    private Double lastPulse;
    private Double lastBreathing;
    private boolean lastBlinkDetected;
    private final Deque<Long> blinkEdges = new ArrayDeque<>();
    private long blinkTrackingStartedMs;
    private long lastEmitMs;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread reader = new Thread(this::run, "presage-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void run() {
        try {
            File exe = new File(executable);
            if (!exe.isFile()) {
                log.error("presage-client not found at {}. Check flowstate.presage.executable "
                        + "(the NMake build puts it at build/presage-client.exe, with no Release/ "
                        + "subfolder), or use VITALS_SOURCE=fake or replay.", exe.getAbsolutePath());
                return;
            }
            if (apiKey == null || apiKey.isBlank()) {
                log.error("flowstate.presage.api-key is empty — presage-client will fail "
                        + "authentication with 401 and produce no readings.");
            }

            ProcessBuilder builder = new ProcessBuilder(exe.getAbsolutePath());
            File workDir = exe.getParentFile();
            if (workDir != null && workDir.isDirectory()) {
                builder.directory(workDir);
            }
            builder.environment().put("SMARTSPECTRA_API_KEY", apiKey == null ? "" : apiKey.trim());

            if (sdkPath != null && !sdkPath.isBlank()) {
                String binDir = new File(sdkPath.trim(), "bin").getAbsolutePath();
                // Windows env var names are case-insensitive, but this map is not,
                // so reuse the existing key rather than assuming it is "PATH".
                String pathKey = builder.environment().keySet().stream()
                        .filter("PATH"::equalsIgnoreCase)
                        .findFirst()
                        .orElse("PATH");
                String existing = builder.environment().getOrDefault(pathKey, "");
                builder.environment().put(pathKey, binDir + File.pathSeparator + existing);
            }

            // presage-client keeps stdout strictly for JSON and sends all of its
            // (very chatty) SDK logging to stderr. INHERIT is not cosmetic: an
            // undrained stderr pipe would fill and block the child mid-session.
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);

            process = builder.start();
            resetReaderState();
            log.info("Started presage-client: {}", exe.getAbsolutePath());

            try (BufferedReader out = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = out.readLine()) != null) {
                    if (!line.startsWith("{")) {
                        continue; // SDK noise that leaked to stdout
                    }
                    handleLine(line);
                }
            }
            log.warn("presage-client exited with code {}", process.waitFor());
        } catch (IOException e) {
            log.error("Could not start presage-client at {}. Use VITALS_SOURCE=fake or replay.",
                    executable, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void resetReaderState() {
        lastPulse = null;
        lastBreathing = null;
        lastBlinkDetected = false;
        blinkEdges.clear();
        blinkTrackingStartedMs = System.currentTimeMillis();
        lastEmitMs = 0L;
    }

    private void handleLine(String line) {
        PresageLine parsed;
        try {
            parsed = jsonMapper.readValue(line, PresageLine.class);
        } catch (Exception e) {
            log.warn("Skipping bad presage line: {}", line);
            return;
        }

        long nowMs = parsed.recordedAt() != null ? parsed.recordedAt() : System.currentTimeMillis();

        // Absent means "no new sample on this line", so hold the previous value
        // rather than writing a null over a perfectly good reading.
        if (parsed.pulseRate() != null) {
            lastPulse = parsed.pulseRate();
        }
        if (parsed.breathingRate() != null) {
            lastBreathing = parsed.breathingRate();
        }

        // Count rising edges only: one blink is a false -> true transition, not
        // every frame that happens to fall inside the same blink.
        boolean blink = Boolean.TRUE.equals(parsed.blinkDetected());
        if (blink && !lastBlinkDetected) {
            blinkEdges.addLast(nowMs);
        }
        lastBlinkDetected = blink;

        while (!blinkEdges.isEmpty() && nowMs - blinkEdges.peekFirst() > BLINK_WINDOW_MS) {
            blinkEdges.removeFirst();
        }

        if (nowMs - lastEmitMs < emitIntervalMs) {
            return;
        }
        lastEmitMs = nowMs;

        vitalsService.save(new VitalReadingDto(lastPulse, lastBreathing, blinkRatePerMinute(nowMs)));
    }

    /**
     * Blinks per minute over the rolling window. Before a full window has
     * elapsed the count is scaled by the time actually observed, so the first
     * minute is not reported as an artificially low blink rate — which matters,
     * because a low blink rate is exactly what Flow State reads as focus.
     * Returns null until there is enough data to extrapolate from honestly.
     */
    private Double blinkRatePerMinute(long nowMs) {
        long observedMs = Math.min(BLINK_WINDOW_MS, Math.max(1L, nowMs - blinkTrackingStartedMs));
        if (observedMs < 5_000L) {
            return null;
        }
        double perMinute = blinkEdges.size() * 60_000.0 / observedMs;
        return Math.round(perMinute * 10) / 10.0;
    }

    @PreDestroy
    public void stop() {
        if (process != null) {
            process.destroy(); // frees the camera
        }
    }
}