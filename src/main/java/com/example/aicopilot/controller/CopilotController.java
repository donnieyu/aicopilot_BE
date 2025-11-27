package com.example.aicopilot.controller;

import com.example.aicopilot.agent.FlowAnalyst;
import com.example.aicopilot.agent.ProcessOutliner;
import com.example.aicopilot.agent.SuggestionAgent;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.analysis.AnalysisReport;
import com.example.aicopilot.dto.analysis.AnalysisResult;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.definition.ProcessStep;
import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import com.example.aicopilot.service.DataContextService;
import com.example.aicopilot.service.JobRepository;
import com.example.aicopilot.service.WorkflowOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final WorkflowOrchestrator orchestrator;
    private final JobRepository jobRepository;
    private final SuggestionAgent suggestionAgent;
    private final ProcessOutliner processOutliner;
    private final FlowAnalyst flowAnalyst;
    private final DataContextService dataContextService;
    private final ObjectMapper objectMapper;

    /**
     * 1. [Mode A] Quick Start (Natural Language -> List -> Map)
     */
    @PostMapping("/start")
    public ResponseEntity<?> startJob(@RequestBody Map<String, String> request) {
        String prompt = request.get("userPrompt");
        String jobId = UUID.randomUUID().toString();
        jobRepository.initJob(jobId);
        orchestrator.runQuickStartJob(jobId, prompt);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId, "message", "Mode A Started"));
    }

    /**
     * 2. [Mode B] Transformation (List JSON -> Map)
     * Requests map generation based on the 'step list' edited by the frontend.
     */
    @PostMapping("/transform")
    public ResponseEntity<?> transformJob(@RequestBody ProcessDefinition definition) {
        String jobId = UUID.randomUUID().toString();
        try {
            String definitionJson = objectMapper.writeValueAsString(definition);
            jobRepository.initJob(jobId);
            orchestrator.runTransformationJob(jobId, definitionJson);
            return ResponseEntity.accepted().body(Map.of("jobId", jobId, "message", "Mode B Started"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid Definition");
        }
    }

    /**
     * 3. Status Check (Polling)
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        JobStatus status = jobRepository.findById(jobId);
        if (status == null) return ResponseEntity.notFound().build();
        String etag = "\"" + status.version() + "\"";
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(status);
    }

    /**
     * 4. Real-time Suggestion (On-Demand) - Graph Context
     */
    @PostMapping("/suggest/graph")
    public ResponseEntity<SuggestionResponse> suggestNextNode(@RequestBody Map<String, String> request) {
        String currentGraphJson = request.get("currentGraphJson");
        String focusNodeId = request.get("focusNodeId");
        String jobId = request.get("jobId");

        String availableVarsJson = "[]";
        if (jobId != null) {
            JobStatus job = jobRepository.findById(jobId);
            if (job != null && job.processResponse() != null && job.dataEntitiesResponse() != null) {
                availableVarsJson = dataContextService.getAvailableVariablesJson(
                        job.processResponse(),
                        job.dataEntitiesResponse(),
                        focusNodeId
                );
            }
        }

        SuggestionResponse response = suggestionAgent.suggestNextSteps(
                currentGraphJson,
                focusNodeId,
                availableVarsJson
        );
        return ResponseEntity.ok(response);
    }

    // [Legacy Support] Keep existing endpoint if needed
    @PostMapping("/suggest")
    public ResponseEntity<SuggestionResponse> suggestLegacy(@RequestBody Map<String, String> request) {
        return suggestNextNode(request);
    }

    /**
     * 5. Outline Suggestion (Drafting Phase)
     * Suggests process steps based on topic and description.
     */
    @PostMapping("/suggest/outline")
    public ResponseEntity<ProcessDefinition> suggestOutline(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        String description = request.get("description");

        ProcessDefinition definition = processOutliner.suggestSteps(topic, description);
        return ResponseEntity.ok(definition);
    }

    /**
     * 6. Step Detail Suggestion API (Micro-Assistant)
     * Suggests a single step using FULL Context (Before & After).
     */
    @PostMapping("/suggest/step")
    public ResponseEntity<ProcessStep> suggestStepDetail(@RequestBody Map<String, Object> request) {
        String topic = (String) request.get("topic");
        String context = (String) request.get("context");
        Integer stepIndex = (Integer) request.get("stepIndex");

        // Receive ALL steps, not just previous ones
        List<Map<String, String>> rawSteps = (List<Map<String, String>>) request.get("currentSteps");

        if (topic == null || stepIndex == null) {
            return ResponseEntity.badRequest().build();
        }

        // Convert raw steps to a summarized list with index markers
        List<String> stepSummaries = List.of();
        if (rawSteps != null) {
            final int[] index = {0};
            stepSummaries = rawSteps.stream()
                    .map(s -> String.format("[%d] %s (%s)", index[0]++, s.get("name"), s.get("role")))
                    .collect(Collectors.toList());
        }

        ProcessStep suggestedStep = processOutliner.suggestSingleStep(
                topic,
                context != null ? context : "",
                stepIndex,
                stepSummaries
        );
        return ResponseEntity.ok(suggestedStep);
    }

    /**
     * 7. [Shadow Architect] Background Analysis Endpoint
     * Triggered by frontend when user is idle after edits.
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeProcess(@RequestBody Map<String, Object> graphSnapshot) {
        try {
            // [Debug] Bean Injection Check
            if (flowAnalyst == null) {
                throw new IllegalStateException("FlowAnalyst bean is not initialized. Please check @AiService configuration.");
            }

            // 프론트에서 보낸 가벼운 스냅샷 데이터 추출
            Object nodesObj = graphSnapshot.get("nodes");
            Object edgesObj = graphSnapshot.get("edges");

            if (nodesObj == null || edgesObj == null) {
                throw new IllegalArgumentException("'nodes' or 'edges' data is missing in the request.");
            }

            String nodesJson = objectMapper.writeValueAsString(nodesObj);
            String edgesJson = objectMapper.writeValueAsString(edgesObj);

            // [Fix] Return wrapped report object instead of raw List to avoid Type Erasure issues in AI Service
            AnalysisReport report = flowAnalyst.analyzeGraph(nodesJson, edgesJson);

            // Unwrap results for frontend
            return ResponseEntity.ok(report.results());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown Error"
            ));
        }
    }
}