package com.example.aicopilot.dto.dataEntities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record LookupItem(
        @JsonProperty("value") @JsonPropertyDescription("The stored value for the lookup option (string or number).") String value,
        @JsonProperty("label") @JsonPropertyDescription("The human-readable label for the lookup option.") String label
) {
}
