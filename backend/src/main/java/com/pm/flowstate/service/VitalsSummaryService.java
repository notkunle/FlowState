package com.pm.flowstate.service;

import com.pm.flowstate.dto.VitalReadingDto;
import com.pm.flowstate.dto.VitalsSummary;
import com.pm.flowstate.model.FocusSession;
import com.pm.flowstate.model.VitalReading;
import com.pm.flowstate.repository.SessionRepository;
import com.pm.flowstate.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class VitalsSummaryService {
    private final SessionRepository sessionRepository;
    private final VitalRepository vitalRepository;
    private final VitalsService vitalsService;

    @Value("${flowstate.vitals.baseline-duration}")
    private Duration baselineDuration;

    @Value("${flowstate.vitals.summary-interval}")
    private Duration summaryInterval;

    // latest summary per session, read by GeminiService
    private final Map<Long, VitalsSummary> latest = new ConcurrentHashMap<>();

    public Optional<VitalsSummary> latest(Long sessionId) {
        return Optional.ofNullable(latest.get(sessionId));
    }

    @Scheduled(fixedRateString = "${flowstate.vitals.summary-interval}")
    public void summarizeActiveSession() {
        sessionRepository.findFirstByEndedAtIsNullOrderByStartedAtDesc().ifPresent(session -> {
            VitalsSummary summary = summarize(session, Instant.now());
            latest.put(session.getId(), summary);
            if (summary.baselineReady()) {
                vitalsService.setBaseline(session.getId(), summary.baseline());
            }
            log.debug("Summary: {}", summary);
        });
    }

    public VitalsSummary summarize(FocusSession session, Instant now) {
        Instant baselineEnd = session.getStartedAt().plus(baselineDuration);
        boolean baselineReady = now.isAfter(baselineEnd);

        VitalReadingDto baseline = average(vitalRepository.findBySessionIdAndRecordedAtBetween(
                session.getId(), session.getStartedAt(), baselineEnd));
        VitalReadingDto recent = average(vitalRepository.findBySessionIdAndRecordedAtBetween(
                session.getId(), now.minus(summaryInterval), now));

        VitalReadingDto changePct = baselineReady ? new VitalReadingDto(
                pct(recent.pulse(), baseline.pulse()),
                pct(recent.breathing(), baseline.breathing()),
                pct(recent.blinks(), baseline.blinks())) : null;

        long minutes = Duration.between(session.getStartedAt(), now).toMinutes();
        return new VitalsSummary(session.getId(), session.getTaskLabel(), minutes,
                baselineReady, baseline, recent, changePct);
    }

    private VitalReadingDto average(List<VitalReading> readings) {
        return new VitalReadingDto(
                avg(readings, VitalReading::getPulse),
                avg(readings, VitalReading::getBreathing),
                avg(readings, VitalReading::getBlinks));
    }

    // average of the non-null values, rounded to one decimal; null if there are none
    private Double avg(List<VitalReading> readings, Function<VitalReading, Double> field) {
        return readings.stream().map(field).filter(v -> v != null)
                .mapToDouble(Double::doubleValue).average()
                .stream().map(v -> Math.round(v * 10) / 10.0).boxed().findFirst().orElse(null);
    }

    private Double pct(Double recent, Double baseline) {
        if (recent == null || baseline == null || baseline == 0) {
            return null;
        }
        return Math.round((recent - baseline) / baseline * 1000) / 10.0;
    }
}
