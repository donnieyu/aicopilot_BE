package com.example.aicopilot.dto.suggestion;

import com.example.aicopilot.dto.process.NodeType;
import com.example.aicopilot.dto.process.config.NodeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Map;

/**
 * Represents a smart suggestion with strongly-typed configuration.
 */
public record NodeSuggestion(
        @JsonProperty("title")
        String title,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("type")
        NodeType type,

        @JsonProperty("configuration")
        @JsonPropertyDescription("Pre-filled, strongly-typed configuration object.")
        NodeConfiguration configuration,

        @JsonProperty("inputMapping")
        Map<String, String> inputMapping
) {}