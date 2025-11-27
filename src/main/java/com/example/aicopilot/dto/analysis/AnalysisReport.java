package com.example.aicopilot.dto.analysis;

import java.util.List;

/**
 * AI Service가 List를 직접 반환할 때 발생하는 Type Erasure/Parsing 문제를 해결하기 위한 래퍼 클래스.
 */
public record AnalysisReport(
        List<AnalysisResult> results
) {}