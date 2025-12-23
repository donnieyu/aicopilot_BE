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
     * Hybrid analysis pipeline execution
     * Layer 1 (Rule-Based) + Layer 2 (AI-Based)
     */
    public List<AnalysisResult> analyze(String nodesJson, String edgesJson) {
        List<AnalysisResult> mergedResults = new ArrayList<>();

        // 1. [Layer 1] Rule-Based Validation (Fast & Free)
        List<AnalysisResult> structuralErrors = validateStructureLocally(nodesJson, edgesJson);
        mergedResults.addAll(structuralErrors);

        // 2. [Layer 2] AI Semantic Analysis (Deep & Insightful)
        try {
            AnalysisReport aiReport = flowAnalyst.analyzeGraph(nodesJson, edgesJson);
            if (aiReport != null && aiReport.results() != null) {
                mergedResults.addAll(aiReport.results());
            }
        } catch (Exception e) {
            log.warn("AI Semantic Analysis failed: {}", e.getMessage());
        }

        return mergedResults;
    }

    /**
     * Local rule check engine
     * [Fixed] All user-facing messages converted to English
     */
    private List<AnalysisResult> validateStructureLocally(String nodesJson, String edgesJson) {
        List<AnalysisResult> errors = new ArrayList<>();
        try {
            List<Map<String, Object>> nodes = objectMapper.readValue(nodesJson, new TypeReference<>() {});
            List<Map<String, Object>> edges = objectMapper.readValue(edgesJson, new TypeReference<>() {});

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

                String label = null;
                Object dataObj = node.get("data");
                if (dataObj instanceof Map) {
                    label = (String) ((Map<?, ?>) dataObj).get("label");
                } else {
                    label = (String) node.get("label");
                }

                // Rule 1: Missing Label
                if (label == null || label.trim().isEmpty()) {
                    errors.add(new AnalysisResult(id, "WARNING", "MISSING_LABEL", "The step name is empty.", "Please provide a clear name for this step."));
                }

                // Rule 2: Connectivity Validation
                boolean isStartNode = "START".equalsIgnoreCase(type) || "start_event".equalsIgnoreCase(type);
                boolean isEndNode = "END".equalsIgnoreCase(type) || "end_event".equalsIgnoreCase(type);

                if (isStartNode) {
                    if (outgoingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "DISCONNECTED_START", "The start point is not connected.", "Please connect the start event to the first step."));
                    }
                } else if (isEndNode) {
                    if (incomingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "DISCONNECTED_END", "The end point is not connected.", "Please connect a line to the end event."));
                    }
                } else {
                    if (incomingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "MISSING_INPUT", "This step is not reachable from the previous stage.", "Please connect a line from the previous step."));
                    }
                    if (outgoingCounts.getOrDefault(id, 0) == 0) {
                        errors.add(new AnalysisResult(id, "ERROR", "MISSING_OUTPUT", "This step has no outgoing connection.", "Please connect this to the next step or the end event."));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Local structural validation failed", e);
        }
        return errors;
    }
}