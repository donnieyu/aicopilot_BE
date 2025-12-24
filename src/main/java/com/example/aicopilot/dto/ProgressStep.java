package com.example.aicopilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 작업의 세부 진행 단계를 나타내는 모델.
 */
public record ProgressStep(
        @JsonProperty("id") String id,        // 내부 식별용 (예: 'val', 'outline')
        @JsonProperty("label") String label,  // UI 출력용 (영문)
        @JsonProperty("status") Status status // 진행 상태
) {
    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    // 상태 변경을 위한 편의 메서드
    public ProgressStep withStatus(Status newStatus) {
        return new ProgressStep(id, label, newStatus);
    }
}