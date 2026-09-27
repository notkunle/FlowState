package com.pm.flowstate.service;

import com.pm.flowstate.dto.GeminiDecision;
import com.pm.flowstate.dto.VitalReadingDto;
import com.pm.flowstate.model.VitalReading;
import com.pm.flowstate.repository.SessionRepository;
import com.pm.flowstate.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class VitalsService {
    private final VitalRepository vitalRepository;
    private final SessionRepository sessionRepository;

    // open SSE connections per session
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // latest baseline per session, set by VitalsSummaryService once the baseline window is over
    private final Map<Long, VitalReadingDto> baselines = new ConcurrentHashMap<>();

    // latest decision per session, set by GeminiService
    private final Map<Long, GeminiDecision> decisions = new ConcurrentHashMap<>();

    // what the frontend receives on every message; baseline + decision are null until they exist
    public record StreamEvent(VitalReadingDto vitals, VitalReadingDto baseline, GeminiDecision decision) {
    }

    // save a reading to the running session and stream it; ignored if no session is running
    public void save(VitalReadingDto dto) {
        save(dto, Instant.now());
    }

    // Same, but with the timestamp the reading was actually taken at.
    // PresageReaderService uses this so the row keeps presage-client's own
    // recordedAt instead of "whenever the backend got around to it" — the gap
    // matters because recordedAt is the hypertable's time column, and
    // VitalsSummaryService's windows are computed from it.
    public void save(VitalReadingDto dto, Instant recordedAt) {
        sessionRepository.findFirstByEndedAtIsNullOrderByStartedAtDesc().ifPresent(session -> {
            vitalRepository.save(new VitalReading(
                    session.getId(), recordedAt,
                    dto.pulse(), dto.breathing(), dto.blinks()));
            send(session.getId(), new StreamEvent(dto, baselines.get(session.getId()), decisions.get(session.getId())));
        });
    }

    public void setBaseline(Long sessionId, VitalReadingDto baseline) {
        baselines.put(sessionId, baseline);
    }

    public void setDecision(Long sessionId, GeminiDecision decision) {
        decisions.put(sessionId, decision);
    }

    public SseEmitter subscribe(Long sessionId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout, lives as long as the page is open
        List<SseEmitter> list = emitters.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> list.remove(emitter));
        emitter.onError(e -> list.remove(emitter));
        return emitter;
    }

    private void send(Long sessionId, StreamEvent event) {
        for (SseEmitter emitter : emitters.getOrDefault(sessionId, List.of())) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e); // browser went away
            }
        }
    }
}