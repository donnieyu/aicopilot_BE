package com.example.aicopilot.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record FormFieldGroup(
        @JsonProperty("id")
        @JsonPropertyDescription("Internal ID. MUST be 'snake_case' (e.g., 'group_requester').")
        String id,

        @JsonProperty("name")
        @JsonPropertyDescription("Concise group name.")
        String name,

        @JsonProperty("description")
        @JsonPropertyDescription("""
                Detailed description (2-3 sentences).
                Explain WHAT the group collects.
                Do NOT describe visibility logic (e.g., never write 'Visible in step 1').
                """)
        String description,

        @JsonProperty("visibleActivityIds") // Renamed from Names -> Ids
        @JsonPropertyDescription("List of 'activityId's where this group is visible.")
        List<String> visibleActivityIds,

        @JsonProperty("fields")
        List<FormField> fields
) {}