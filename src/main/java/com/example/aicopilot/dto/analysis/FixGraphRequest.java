package com.example.aicopilot.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FixGraphRequest(
        Map<String, Object> graphSnapshot, // Contains "nodes" and "edges" lists
        AnalysisResult error // The specific error to fix
) {}