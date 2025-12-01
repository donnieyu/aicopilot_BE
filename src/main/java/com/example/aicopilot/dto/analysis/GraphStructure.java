package com.example.aicopilot.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GraphStructure(
        List<Map<String, Object>> nodes,
        List<Map<String, Object>> edges
) {}