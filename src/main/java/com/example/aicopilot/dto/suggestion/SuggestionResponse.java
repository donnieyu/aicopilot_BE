package com.example.aicopilot.dto.suggestion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record SuggestionResponse(
        @JsonProperty("suggestions")
        @JsonPropertyDescription("List of recommended next actions.")
        List<NodeSuggestion> suggestions
) {}