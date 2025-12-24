package com.example.aicopilot.dto;

import com.example.aicopilot.dto.analysis.AnalysisResult;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JobStatus 모델 (Ver 11.2 - Analysis Results Support)
 * 모든 설계 작업의 최종 분석 결과(analysisResults)를 포함합니다.
 */
public record JobStatus(
        String jobId,
        State state,
        String message,
        String lastUpdatedStage,
        long version,

        long startTime,
        Map<String, Long> stageDurations,
        long totalElapsedMillis,

        List<ProgressStep> progressSteps,

        // [New] 최종 오딧(Audit) 결과 리스트
        List<AnalysisResult> analysisResults,

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
                new ArrayList<>(),
                new ArrayList<>(), // 초기 빈 리스트
                null, null, null
        );
    }
}