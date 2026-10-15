package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Identifier (email or username) is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
}