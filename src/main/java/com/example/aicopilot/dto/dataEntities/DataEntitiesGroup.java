package com.example.aicopilot.dto.dataEntities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Model for DataEntitiesGroup
 *
 * @author Donnie Yu
 * @version 26.0.0
 * @since 26.0.0
 */
public record DataEntitiesGroup(
        @JsonProperty("id") @JsonPropertyDescription("A unique identifier for the data entities group model, used for system-level processes like data conversion or error handling.") String id,
        @JsonProperty("alias") @JsonPropertyDescription("A unique identifier for the group, used within the product for business logic and user recognition.") String alias,
        @JsonProperty("name") @JsonPropertyDescription("A descriptive name for the group of entities.") String name,
        @JsonProperty("description") @JsonPropertyDescription("Brief explanation of the group of entities.") String description,
        @JsonProperty("entityIds") @JsonPropertyDescription("An array of entity ids within this group.") List<String> entityIds
) {
}
