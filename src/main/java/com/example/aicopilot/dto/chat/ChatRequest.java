package com.example.aicopilot.dto.chat;

import java.util.List;

/**
 * Extended ChatRequest to support incremental updates.
 * Includes the current state of the workflow canvas.
 */
public record ChatRequest(
        String userPrompt,
        List<String> selectedAssetIds,

        // [New] The current process map in JSON format for the MODIFY intent.
        String currentProcessJson
) {}