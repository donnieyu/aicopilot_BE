package com.example.aicopilot.dto.dataEntities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Model for DataEntity
 * [Sync] Aligned with DataModeler v26.2.0 (Migration Phase 2)
 */
public record DataEntity(
        @JsonProperty("id")
        @JsonPropertyDescription("Internal ID. MUST be 'snake_case' (e.g., 'work_email').")
        String id,

        @JsonProperty("alias")
        @JsonPropertyDescription("""
                Public Key for Form Mapping & Smart Binding.
                MUST be 'UpperCamelCase' (e.g., 'WorkEmail').
                NO underscores, NO numbers at start.
                """)
        String alias,

        @JsonProperty("sourceNodeId")
        @JsonPropertyDescription("""
                [NEW] The ID of the Activity Node that generates or captures this data.
                This creates a lineage record within the Global Data Pool.
                - Input data from a form -> Use User Task ID.
                - Output from a system action -> Use Service Task ID.
                - Global Constants -> null.
                """)
        String sourceNodeId,

        @JsonProperty("label")
        @JsonPropertyDescription("""
                Human-readable Label. Use 'Title Case' (e.g., 'Work Email').
                MUST be human-readable.
                """)
        String label,

        @JsonProperty("type")
        @JsonPropertyDescription("""
                Data Type. Use exact lowercase enum value (e.g., 'string', 'lookup', 'file').
                - Use 'lookup' for static sets (Yes/No, Currency).
                - Use 'string' for dynamic text.
                - Use 'sign' for approvals.
                """)
        DataEntityType type,

        @JsonProperty("description")
        @JsonPropertyDescription("Brief explanation of the entity's purpose.")
        String description,

        @JsonProperty("required")
        @JsonPropertyDescription("Default: false. Set true only if mandatory.")
        boolean required,

        @JsonProperty("isPrimaryKey")
        @JsonPropertyDescription("Default: false. Do NOT set true for user inputs.")
        boolean isPrimaryKey,

        @JsonProperty("maxLength")
        @JsonPropertyDescription("""
                MANDATORY for 'string' and 'string_array' types.
                (e.g., 20 for codes, 100 for names, 255 for general text).
                """)
        Integer maxLength,

        @JsonProperty("lookupData")
        @JsonPropertyDescription("""
                MANDATORY if type is 'lookup' or 'lookup_array'.
                'lookupItems' must be non-empty.
                """)
        LookupData lookupData,

        @JsonProperty("pattern")
        @JsonPropertyDescription("Optional Regex for validation (e.g., email, phone).")
        String pattern,

        @JsonProperty("requireTrue")
        @JsonPropertyDescription("""
                Logic:
                - true: User MUST select 'True/Yes' (Mandatory Agreement).
                - false: User can select 'True' or 'False' (Standard Boolean).
                """)
        Boolean requireTrue
) {}