package com.pm.flowstate.config;

import com.pm.flowstate.model.FocusSession;
import com.pm.flowstate.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// fake week of history so the insights chart has something to show in the demo
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {
    private static final int DAYS = 7;
    private static final int[] SESSION_HOURS = {9, 11, 14, 16, 20}; // local start hours
    private static final String[] TASKS = {"Write report", "Study for midterm", "Fix login bug", "Read paper", "Plan sprint"};
    private static final Duration DECISION_EVERY = Duration.ofSeconds(90);

    private final SessionRepository sessionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Instant dayAgo = Instant.now().minus(Duration.ofDays(1));
        if (sessionRepository.existsByStartedAtBefore(dayAgo)) {
            return; // history already there
        }

        Random random = new Random(42); // same fake week every time
        ZoneId zone = ZoneId.systemDefault();
        List<Object[]> decisions = new ArrayList<>();

        for (int day = DAYS; day >= 1; day--) {
            LocalDate date = LocalDate.now(zone).minusDays(day);
            for (int i = 0; i < SESSION_HOURS.length; i++) {
                int hour = SESSION_HOURS[i];
                Instant start = date.atTime(hour, random.nextInt(30)).atZone(zone).toInstant();
                Instant end = start.plus(Duration.ofMinutes(45 + random.nextInt(46)));

                FocusSession session = new FocusSession(TASKS[i], start);
                session.setEndedAt(end);
                session = sessionRepository.save(session);

                for (Instant t = start.plus(Duration.ofMinutes(3)); t.isBefore(end); t = t.plus(DECISION_EVERY)) {
                    String state = pickState(hour, random);
                    decisions.add(new Object[]{session.getId(), Timestamp.from(t), state, reasonFor(state), null});
                }
            }
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO state_decision (session_id, decided_at, state, reason, suggestion) VALUES (?, ?, ?, ?, ?)",
                decisions);
        log.info("Seeded {} days of history ({} decisions)", DAYS, decisions.size());
    }

    // mornings and mid-afternoon focus best, evenings are more stressed
    private String pickState(int hour, Random random) {
        double focus = switch (hour) {
            case 9, 11 -> 0.7;
            case 14 -> 0.45;
            case 16 -> 0.6;
            default -> 0.3;
        };
        double r = random.nextDouble();
        if (r < focus) return "focus";
        if (r < focus + (hour >= 20 ? 0.4 : 0.15)) return "stress";
        return "neutral";
    }

    private String reasonFor(String state) {
        return switch (state) {
            case "focus" -> "Steady pulse and fewer blinks than baseline.";
            case "stress" -> "Pulse and breathing up versus baseline.";
            default -> "Vitals close to baseline.";
        };
    }
}
