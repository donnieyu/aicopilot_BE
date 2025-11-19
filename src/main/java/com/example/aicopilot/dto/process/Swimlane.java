package com.example.aicopilot.dto.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Model for Swimlane
 *
 * @author Donnie Yu
 * @version 26.0.0
 * @since 26.0.0
 */
public record Swimlane(
        @JsonProperty("swimlaneId") @JsonPropertyDescription("A unique identifier for the swimlane (e.g., 'requester', 'legal').") String swimlaneId,
        @JsonProperty("name") @JsonPropertyDescription("A human-readable name for the swimlane (e.g., 'Requester', 'Legal Department').") String name,
        @JsonProperty("nextSwimlaneId") @JsonPropertyDescription("""
                The `swimlaneId` of the next step in the process flow, ensuring a forward-moving sequence.
                It must point to a subsequent swimlane.
                Set to `null` for the final swimlane.""")
                String nextSwimlaneId
) {
}
