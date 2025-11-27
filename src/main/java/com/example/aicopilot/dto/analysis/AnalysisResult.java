package com.example.aicopilot.dto.analysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisResult(
        String targetNodeId,   // 문제가 발견된 노드 ID (null이면 전역 문제)
        String severity,       // "ERROR", "WARNING", "INFO"
        String type,           // "MISSING_FLOW", "DATA_ISSUE", "OPTIMIZATION"
        String message,        // 사용자에게 보여줄 메시지 (예: "승인 후 반려 경로가 없습니다.")
        String suggestion      // (선택) 간단한 해결 제안
) {}