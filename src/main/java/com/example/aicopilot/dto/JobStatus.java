package com.example.aicopilot.dto;

import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;

/**
 * 작업 상태 및 단계별 결과물 저장소
 */
public record JobStatus(
        String jobId,
        State state,
        String message,

        // Stage 1 결과
        ProcessResponse processResponse,

        // Stage 2 결과
        DataEntitiesResponse dataEntitiesResponse,

        // Stage 3 결과
        FormResponse formResponse
) {
    public enum State {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public static JobStatus init(String jobId) {
        return new JobStatus(jobId, State.PENDING, "작업 대기 중...", null, null, null);
    }
}