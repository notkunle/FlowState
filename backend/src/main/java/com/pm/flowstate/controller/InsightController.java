package com.pm.flowstate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {
    private final JdbcTemplate jdbcTemplate;

    public record HourlyFocus(String hour, int avgFocusScore, int sessionCount) {
    }

    // GET /api/insights/hourly-focus?from=2026-09-01&to=2026-09-30
    // avgFocusScore = % of decisions in that hour of day that were "focus"
    @GetMapping("/hourly-focus")
    public List<HourlyFocus> hourlyFocus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate end = to != null ? to.plusDays(1) : LocalDate.now(zone).plusDays(1);
        LocalDate start = from != null ? from : end.minusDays(31);

        int[] score = new int[24];
        int[] sessions = new int[24];
        jdbcTemplate.query("""
                        SELECT extract(hour FROM bucket AT TIME ZONE ?)::int AS hour,
                               round(100.0 * sum(focus_count) / nullif(sum(decision_count), 0))::int AS score,
                               count(DISTINCT session_id)::int AS sessions
                        FROM hourly_focus
                        WHERE bucket >= ? AND bucket < ?
                        GROUP BY 1
                        """,
                rs -> {
                    score[rs.getInt("hour")] = rs.getInt("score");
                    sessions[rs.getInt("hour")] = rs.getInt("sessions");
                },
                zone.getId(),
                Timestamp.from(start.atStartOfDay(zone).toInstant()),
                Timestamp.from(end.atStartOfDay(zone).toInstant()));

        // always 24 rows so the chart has every hour
        List<HourlyFocus> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new HourlyFocus("%02d:00".formatted(h), score[h], sessions[h]));
        }
        return result;
    }
}
