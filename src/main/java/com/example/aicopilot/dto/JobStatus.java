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

        // [NEW] 시간 측정 필드
        long startTime, // 시작 시간 (Epoch)
        Map<String, Long> stageDurations, // 단계별 소요 시간 (ms)
        long totalElapsedMillis, // 총 경과 시간

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
                "작업 대기 중...",
                "INIT",
                0L,
                System.currentTimeMillis(),
                Map.of(),
                0L,
                null, null, null
        );
    }
}