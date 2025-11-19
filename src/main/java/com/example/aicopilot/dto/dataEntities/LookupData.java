package com.example.aicopilot.dto.dataEntities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record LookupData(
        @JsonProperty("name")
        @JsonPropertyDescription("""
                Human-readable name for the category.
                Use spaces, Title Case (e.g., 'Expense Type').
                Do NOT use code style (e.g., 'expense_type').
                """)
        String name,

        @JsonProperty("description")
        @JsonPropertyDescription("Brief explanation of the lookup data.")
        String description,

        @JsonProperty("lookupItems")
        @JsonPropertyDescription("""
                List of options. Must not be empty.
                Each item must have 'value' (code) and 'label' (display text).
                """)
        List<LookupItem> lookupItems
) {}