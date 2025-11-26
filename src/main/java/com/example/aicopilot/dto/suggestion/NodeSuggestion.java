package com.example.aicopilot.dto.suggestion;

import com.example.aicopilot.dto.process.NodeType;
import com.example.aicopilot.dto.process.config.NodeConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Map;

/**
 * Represents a smart suggestion with strongly-typed configuration and Data Binding.
 * [Fix] @JsonIgnoreProperties(ignoreUnknown = true) 추가
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NodeSuggestion(
        @JsonProperty("title")
        @JsonPropertyDescription("Short title of the suggestion (e.g., 'Add Manager Approval').")
        String title,

        @JsonProperty("reason")
        @JsonPropertyDescription("Why AI recommends this step (e.g., 'Expense > $1000 usually requires approval').")
        String reason,

        @JsonProperty("type")
        NodeType type,

        @JsonProperty("configuration")
        @JsonPropertyDescription("Pre-filled configuration. Use this to auto-populate the node settings panel.")
        NodeConfiguration configuration,

        @JsonProperty("inputMapping")
        @JsonPropertyDescription("""
                Smart Binding Proposals.
                Key: The input field name of the NEW suggested node.
                Value: The binding expression `{{ NodeID.VariableKey }}` pointing to an EXISTING node.
                """)
        Map<String, String> inputMapping
) {}