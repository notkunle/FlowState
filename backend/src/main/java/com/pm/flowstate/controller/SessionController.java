package com.pm.flowstate.controller;


import com.pm.flowstate.model.FocusSession;
import com.pm.flowstate.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // GET /api/sessions/active
    // Lets the frontend re-attach to a session that is already running — after a
    // page refresh, or when the backend was started before the browser. Without
    // this the UI can get permanently stuck: the session exists, so POST
    // /api/sessions returns 409, but the UI has no session id so it never opens
    // the vitals stream and shows "No active session" while data flows fine
    // behind it.
    // Returns 204 No Content when nothing is running, so the frontend can tell
    // "no session" apart from an error.
    @GetMapping("/active")
    public ResponseEntity<FocusSession> active() {
        return sessionRepository.findFirstByEndedAtIsNullOrderByStartedAtDesc()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
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