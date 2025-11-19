package com.example.aicopilot.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Model for FormResponse
 *
 * @author Donnie Yu
 * @version 26.0.0
 * @since 26.0.0
 */
public record FormResponse(
        @JsonProperty("formDefinitions")
        @JsonPropertyDescription("An array containing a single form definition object.")
        List<FormDefinitions> formDefinitions
) {
}
