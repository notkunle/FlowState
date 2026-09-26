package com.pm.flowstate.dto;

// Gemini's structured answer: state is focus, stress or neutral
public record GeminiDecision(
        String state,
        String reason,
        String suggestion
) {
}
