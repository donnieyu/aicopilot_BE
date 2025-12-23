package com.example.aicopilot.dto.chat; // Fixed package

import java.util.List;

public record ChatRequest(
        String userPrompt,          // Frontend uses 'userPrompt'
        List<String> selectedAssetIds
) {}