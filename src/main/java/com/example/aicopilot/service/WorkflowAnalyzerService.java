package com.example.aicopilot.service;

import com.example.aicopilot.agent.FlowAnalyst;
import com.example.aicopilot.dto.analysis.AnalysisReport;
import com.example.aicopilot.dto.analysis.AnalysisResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAnalyzerService {

    private final FlowAnalyst flowAnalyst;
    private final ObjectMapper objectMapper;

    /**
     * 하이브리드 분석 파이프라인 실행
     * Layer 1 (Rule-Based) + Layer 2 (AI-Based)
     */
    public List<AnalysisResult> analyze(String nodesJson, String edgesJson) {
        List<AnalysisResult> mergedResults = new ArrayList<>();

        // 1. [Layer 1] Rule-Based Validation (Fast & Free)
        // 자바 로직으로 명확한 구조적 오류(끊긴 연결, 필수 값 누락)를 0ms 수준에서 감지합니다.
        List<AnalysisResult> structuralErrors = validateStructureLocally(nodesJson, edgesJson);
        mergedResults.addAll(structuralErrors);

        // 구조적 에러가 너무 많으면(예: 3개 이상) AI 분석을 스킵하여 토큰 비용을 절약하는 전략도 가능합니다.
        // 현재는 항상 실행하도록 설정합니다.

        // 2. [Layer 2] AI Semantic Analysis (Deep & Insightful)
        try {
            AnalysisReport aiReport = flowAnalyst.analyzeGraph(nodesJson, edgesJson);
            if (aiReport != null && aiReport.results() != null) {
                mergedResults.addAll(aiReport.results());
            }
        } catch (Exception e) {
            // AI 분석 실패는 전체 로직을 방해하지 않도록 로그만 남기고, 구조적 에러만 반환합니다.
            log.warn("AI Semantic Analysis failed: {}", e.getMessage());
        }

        return mergedResults;
    }

    /**
     * 로컬 규칙 검사 엔진
     * JSON 데이터를 파싱하여 그래프의 연결성(Connectivity)과 무결성을 검증합니다.
     */
    private List<AnalysisResult> validateStructureLocally(String nodesJson, String edgesJson) {
        List<AnalysisResult> errors = new ArrayList<>();
        try {
            List<Map<String, Object>> nodes = objectMapper.readValue(nodesJson, new TypeReference<>() {});
            List<Map<String, Object>> edges = objectMapper.readValue(edgesJson, new TypeReference<>() {});

            // 그래프 연결성 분석을 위한 카운터 맵 생성
            Map<String, Integer> incomingCounts = new HashMap<>();
            Map<String, Integer> outgoingCounts = new HashMap<>();

            for (Map<String, Object> edge : edges) {
                String source = (String) edge.get("source");
                String target = (String) edge.get("target");
                outgoingCounts.merge(source, 1, Integer::sum);
                incomingCounts.merge(target, 1, Integer::sum);
            }

            for (Map<String, Object> node : nodes) {
                String id = (String) node.get("id");
                String type = (String) node.get("type");

                // Frontend snapshot 구조에 따라 데이터 추출 (label이 data 내부에 있을 수 있음)
                String label = null;
                Object dataObj = node.get("data"); // ReactFlow data object
                if (dataObj instanceof Map) {
                    label = (String) ((Map<?, ?>) dataObj).get("label");
                } else {
                    label = (String) node.get("label"); // Fallback
                }

                // Rule 1: 필수 값 검증 (이름 누락)
                if (label == null || label.trim().isEmpty()) {
                    errors.add(new AnalysisResult(id, "WARNING", "MISSING_LABEL", "단계의 이름이 비어있습니다.", "명확한 이름을 입력해주세요."));
                }

                // Rule 2: 연결성 검증 (Connectivity)
                boolean isStartNode = "START".equalsIgnoreCase(type) || "start_event".equalsIgnoreCase(type);
                boolean isEndNode = "END".equalsIgnoreCase(type) || "end_event".equalsIgnoreCase(type);

                if (isStartNode) {
                    if (outgoingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "DISCONNECTED_START", "시작점이 연결되지 않았습니다.", "첫 번째 단계와 연결선을 그려주세요."));
                    }
                } else if (isEndNode) {
                    if (incomingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "DISCONNECTED_END", "종료점이 연결되지 않았습니다.", "마지막 단계로부터 선을 연결해주세요."));
                    }
                } else {
                    // 일반 노드 (User Task, Service Task, Gateway)
                    if (incomingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "MISSING_INPUT", "이전 단계와 연결되지 않았습니다.", "이전 단계에서 선을 연결해주세요."));
                    }
                    if (outgoingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "MISSING_OUTPUT", "다음 단계와 연결되지 않았습니다.", "다음 단계로 선을 연결하거나 종료 노드와 연결하세요."));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Local structural validation failed", e);
            // 파싱 에러 시에는 빈 리스트 반환 (AI 분석에 의존하거나 다음 주기에 재시도)
        }
        return errors;
    }
}