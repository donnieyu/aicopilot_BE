package com.example.aicopilot.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Model for FormField
 * [Sync] Updated to align with FormUXDesigner v14.0 System Message.
 */
public record FormField(
        @JsonProperty("id")
        @JsonPropertyDescription("Internal ID. MUST be 'snake_case' (e.g., 'field_start_date').")
        String id,

        @JsonProperty("entityAlias")
        @JsonPropertyDescription("""
                CRITICAL: Must match DataEntity 'alias' EXACTLY (Case-Sensitive).
                e.g., Data='VacationDays' -> Form='VacationDays'.
                """)
        String entityAlias,

        @JsonProperty("label")
        @JsonPropertyDescription("Display Label on the form.")
        String label,

        @JsonProperty("component")
        @JsonPropertyDescription("""
                UI Component.
                - string (<100) -> input_text | (>100) -> input_textarea
                - lookup -> dropdown
                - *_array -> chips / multiple_dropdown
                - file -> file_upload / file_list
                """)
        FormFieldComponent component,

        @JsonProperty("required")
        boolean required,

        @JsonProperty("visibleActivityIds") // Renamed from Names -> Ids
        @JsonPropertyDescription("""
                List of 'activityId's (NOT Names) where this field is visible.
                MUST be a subset of Parent Group's visibility.
                """)
        List<String> visibleActivityIds,

        @JsonProperty("readonlyActivityIds") // Renamed from Names -> Ids
        @JsonPropertyDescription("""
                List of 'activityId's where this field is read-only.
                MUST be a subset of visibleActivityIds.
                EXCEPTION: 'file_list' component should NEVER be readonly (must be interactive).
                """)
        List<String> readonlyActivityIds
) {}