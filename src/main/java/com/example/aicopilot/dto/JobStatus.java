package com.example.aicopilot.dto;

import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;

import java.util.Map;

public record JobStatus(
        String jobId,
        State state,
        String message,
        String lastUpdatedStage,
        long version,

        // Time measurement fields
        long startTime, // Start time (Epoch)
        Map<String, Long> stageDurations, // Duration per stage (ms)
        long totalElapsedMillis, // Total elapsed time

        ProcessResponse processResponse,
        DataEntitiesResponse dataEntitiesResponse,
        FormResponse formResponse
) {
    public enum State {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public static JobStatus init(String jobId) {
        return new JobStatus(
                jobId,
                State.PENDING,
                "Waiting for job...",
                "INIT",
                0L,
                System.currentTimeMillis(),
                Map.of(),
                0L,
                null, null, null
        );
    }
}