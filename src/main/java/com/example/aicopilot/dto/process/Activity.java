package com.example.aicopilot.dto.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Model for Activity
 * [Sync] Aligned with ProcessArchitect v14.0 System Message
 */
public record Activity(
        @JsonProperty("activityId")
        @JsonPropertyDescription("""
                Internal Identifier. MUST be 'snake_case' (e.g., 'submit_req').
                MUST be Globally Unique within the process.
                """)
        String activityId,

        @JsonProperty("activityName")
        @JsonPropertyDescription("""
                Human-readable Name. MUST be Globally Unique within the process.
                Avoid generic names like 'Review'; use specific names like 'HR Review'.
                """)
        String activityName,

        @JsonProperty("participant")
        @JsonPropertyDescription("""
                Role responsible for this activity.
                First Activity MUST be 'Initiator'.
                Approvals SHOULD start with 'Manager of Initiator'.
                """)
        String participant,

        @JsonProperty("swimlaneId")
        @JsonPropertyDescription("Link to the parent Swimlane ID.")
        String swimlaneId,

        @JsonProperty("description")
        @JsonPropertyDescription("""
                List of specific participant actions (e.g., 'Check receipt', 'Approve request').
                MUST NOT be a single string.
                """)
        List<String> description,

        @JsonProperty("nextActivityId")
        @JsonPropertyDescription("""
                Pointer to the NEXT activity.
                MUST point forward (to an activity in the same or next swimlane).
                Set to null for the final activity.
                """)
        String nextActivityId,

        @JsonProperty("rejectActivityId")
        @JsonPropertyDescription("""
                Pointer to the PREVIOUS activity (for rejection).
                MUST point backward (to an activity in the same or previous swimlane).
                """)
        String rejectActivityId
) {}