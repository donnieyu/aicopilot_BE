package com.example.aicopilot.dto.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Model for ProcessResponse
 *
 * @author Donnie Yu
 * @version 26.0.0
 * @since 26.0.0
 */
public record ProcessResponse(
        @JsonProperty("processName")
        @JsonPropertyDescription("The official, human-readable name or title of the entire business process (e.g., 'Employee Expense Reimbursement', 'New Client Onboarding').")
        String processName,
        @JsonProperty("description")
        @JsonPropertyDescription("A one or two-sentence summary that explains the purpose and scope of the process.")
        String description,
        @JsonProperty("swimlanes")
        @JsonPropertyDescription("An ordered array of swimlanes. The order of this array defines the major stages of the process, from start to finish.")
        List<Swimlane> swimlanes,
        @JsonProperty("activities")
        @JsonPropertyDescription("""
                An ordered list of all activities in the process.
                The sequence of activities MUST follow the forward-moving flow defined by the `swimlanes` order and the `nextActivityId` links.
                """)
        List<Activity> activities
) {
}
