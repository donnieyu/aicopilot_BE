package com.example.aicopilot.dto;

import java.util.List;

public record ChatRequest(
        String userPrompt,
        List<String> selectedAssetIds
) {}