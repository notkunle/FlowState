package com.pm.flowstate.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.pm.flowstate.dto.GeminiDecision;
import com.pm.flowstate.dto.VitalReadingDto;
import com.pm.flowstate.dto.VitalsSummary;
import com.pm.flowstate.model.StateDecision;
import com.pm.flowstate.repository.DecisionRepository;
import com.pm.flowstate.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {
    private static final List<String> STATES = List.of("focus", "stress", "neutral");

    // Gemini must answer with exactly this JSON shape
    private static final Schema DECISION_SCHEMA = Schema.builder()
            .type(Type.Known.OBJECT)
            .properties(Map.of(
                    "state", Schema.builder().type(Type.Known.STRING).enum_(STATES).build(),
                    "reason", Schema.builder().type(Type.Known.STRING).build(),
                    "suggestion", Schema.builder().type(Type.Known.STRING).build()))
            .required("state", "reason", "suggestion")
            .build();

    private final ObjectProvider<Client> geminiClient; // empty when no API key is set
    private final SessionRepository sessionRepository;
    private final VitalsSummaryService summaryService;
    private final DecisionRepository decisionRepository;
    private final VitalsService vitalsService;
    private final JsonMapper jsonMapper;

    @Value("${flowstate.gemini.model}")
    private String model;

    @Async // Gemini can take a few seconds; don't block the scheduler thread
    @Scheduled(fixedRateString = "${flowstate.gemini.decision-interval}")
    public void decideForActiveSession() {
        sessionRepository.findFirstByEndedAtIsNullOrderByStartedAtDesc()
                .flatMap(session -> summaryService.latest(session.getId()))
                .filter(VitalsSummary::baselineReady) // nothing to compare against yet
                .ifPresent(summary -> {
                    GeminiDecision decision = decide(summary);
                    decisionRepository.save(new StateDecision(summary.sessionId(), Instant.now(),
                            decision.state(), decision.reason(), decision.suggestion()));
                    vitalsService.setDecision(summary.sessionId(), decision);
                    log.info("Decision for session {}: {}", summary.sessionId(), decision);
                });
    }

    public GeminiDecision decide(VitalsSummary summary) {
        Client client = geminiClient.getIfAvailable();
        if (client == null) {
            return fallback(summary);
        }
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(DECISION_SCHEMA)
                    .temperature(0.2f)
                    .build();
            String json = client.models.generateContent(model, prompt(summary), config).text();
            GeminiDecision decision = jsonMapper.readValue(json, GeminiDecision.class);
            return STATES.contains(decision.state()) ? decision : fallback(summary);
        } catch (Exception e) {
            log.warn("Gemini call failed, using fallback: {}", e.getMessage());
            return fallback(summary);
        }
    }

    private String prompt(VitalsSummary s) {
        return """
                You are Flow State, a focus coach. From webcam vitals, decide the user's current state.
                Judge only relative to the user's own baseline (their first minutes of this session);
                this is not a medical assessment.

                Task: %s
                Minutes into session: %d
                Baseline averages: %s
                Last 30s averages: %s
                Change vs baseline (%%): %s

                Units: pulse in beats/min, breathing in breaths/min, blinks in blinks/min.
                Rough guide: pulse and breathing up with more blinking suggests stress;
                steady pulse and breathing with fewer blinks suggests focus.

                Answer with:
                - state: focus, stress or neutral
                - reason: one short sentence that cites the numbers
                - suggestion: one short, friendly action (suggest a short break if stress)
                """.formatted(s.taskLabel(), s.minutesIntoSession(),
                format(s.baseline()), format(s.recent()), format(s.changePct()));
    }

    private String format(VitalReadingDto v) {
        return "pulse=%s, breathing=%s, blinks=%s".formatted(v.pulse(), v.breathing(), v.blinks());
    }

    // simple rules so the demo still works without a key or when Gemini is down
    private GeminiDecision fallback(VitalsSummary s) {
        VitalReadingDto c = s.changePct();
        double pulse = c.pulse() == null ? 0 : c.pulse();
        double breathing = c.breathing() == null ? 0 : c.breathing();
        double blinks = c.blinks() == null ? 0 : c.blinks();

        if (pulse > 8 || breathing > 10 || blinks > 25) {
            return new GeminiDecision("stress",
                    "Pulse %+.0f%%, breathing %+.0f%%, blinks %+.0f%% vs your baseline.".formatted(pulse, breathing, blinks),
                    "Take a 2-minute break: stand up and breathe slowly.");
        }
        if (blinks < -15 && Math.abs(pulse) < 5) {
            return new GeminiDecision("focus",
                    "Steady pulse and %.0f%% fewer blinks than your baseline.".formatted(-blinks),
                    "You're in the zone, keep going.");
        }
        return new GeminiDecision("neutral",
                "Vitals are close to your baseline.",
                "Carry on with your task.");
    }
}
