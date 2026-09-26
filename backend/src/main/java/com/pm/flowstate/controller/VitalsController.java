package com.pm.flowstate.controller;

import com.pm.flowstate.service.VitalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class VitalsController {
    private final VitalsService vitalsService;

    // GET /api/sessions/1/vitals/stream  (EventSource in the frontend)
    @GetMapping(path = "/{id}/vitals/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        return vitalsService.subscribe(id);
    }
}
