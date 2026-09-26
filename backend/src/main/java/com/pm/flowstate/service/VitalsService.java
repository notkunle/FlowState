package com.pm.flowstate.service;

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

    // what the frontend receives on every message; baseline + decision are null until they exist
    public record StreamEvent(VitalReadingDto vitals, Object baseline, Object decision) {
    }

    // save a reading to the running session and stream it; ignored if no session is running
    public void save(VitalReadingDto dto) {
        sessionRepository.findFirstByEndedAtIsNullOrderByStartedAtDesc().ifPresent(session -> {
            vitalRepository.save(new VitalReading(
                    session.getId(), Instant.now(),
                    dto.pulse(), dto.breathing(), dto.blinks()));
            send(session.getId(), new StreamEvent(dto, baselines.get(session.getId()), null));
        });
    }

    public void setBaseline(Long sessionId, VitalReadingDto baseline) {
        baselines.put(sessionId, baseline);
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
