package com.pm.flowstate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartSessionRequest(
        @NotBlank @Size(max = 255) String taskLabel
) {
}
