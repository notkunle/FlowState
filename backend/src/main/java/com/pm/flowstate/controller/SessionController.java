package com.pm.flowstate.controller;


import com.pm.flowstate.model.FocusSession;
import com.pm.flowstate.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;



@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionRepository sessionRepository;


    // POST /api/sessions?taskLabel=write report
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FocusSession start(@RequestParam String taskLabel) {
        if (taskLabel.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskLabel is required");
        }
        if (sessionRepository.findFirstByEndedAtIsNullOrderByStartedAtDesc().isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A session is already running");
        }
        return sessionRepository.save(new FocusSession(taskLabel, Instant.now()));
    }

    // POST /api/sessions/1/stop
    @PostMapping("/{id}/stop")
    public FocusSession stop(@PathVariable Long id) {
        FocusSession session = findSession(id);
        if (session.isActive()) {
            session.setEndedAt(Instant.now());
            session = sessionRepository.save(session);
        }
        return session;
    }

    // GET /api/sessions/1
    @GetMapping("/{id}")
    public FocusSession get(@PathVariable Long id) {
        return findSession(id);
    }

    private FocusSession findSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Session " + id + " not found"));
    }
    }



