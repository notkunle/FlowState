package com.pm.flowstate.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// every API error comes back as {"status": 404, "error": "Session 9 not found"}
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException e) {
        return body(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException e) {
        return body(HttpStatus.BAD_REQUEST, e.getParameterName() + " is required");
    }

    // browser closed an SSE stream; nothing to send back
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClosedStream() {
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
        log.error("Unexpected error", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String error) {
        return ResponseEntity.status(status)
                .body(Map.of("status", status.value(), "error", error == null ? status.getReasonPhrase() : error));
    }
}
