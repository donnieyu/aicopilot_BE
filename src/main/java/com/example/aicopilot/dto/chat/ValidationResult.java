package com.example.aicopilot.dto.chat;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Record holding the result of the domain guardrail validation.
 * Used by InputGuardAgent to determine if a query should proceed.
 */
public record ValidationResult(
        @JsonPropertyDescription("Status of the validation: VALID, INVALID, or BRIDGE.")
        ValidationStatus status,

        @JsonPropertyDescription("A descriptive guidance message for the user, especially when status is not VALID.")
        String message
) {
    /**
     * Enum for validation status codes.
     */
    public enum ValidationStatus {
        VALID, INVALID, BRIDGE
    }
}