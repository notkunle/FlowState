package com.pm.flowstate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

// aliases match presage-client's JSON: {"timestamp":..,"pulseBpm":..,"blinkRate":..,"confidence":..}
public record VitalReadingDto(
        @JsonAlias("pulseBpm") Double pulse,       // beats per minute
        Double breathing,                          // breaths per minute (presage-client doesn't send it yet)
        @JsonAlias("blinkRate") Double blinks      // blinks per minute
) {
}
