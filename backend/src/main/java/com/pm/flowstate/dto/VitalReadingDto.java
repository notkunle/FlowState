package com.pm.flowstate.dto;

public record VitalReadingDto(
        Double pulse,      // beats per minute
        Double breathing,  // breaths per minute
        Double blinks      // blinks per minute
) {
}
