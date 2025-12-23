package com.example.aicopilot.controller;

import com.example.aicopilot.agent.*;
import com.example.aicopilot.dto.*;
import com.example.aicopilot.dto.analysis.*;
import com.example.aicopilot.dto.chat.ChatRequest; // [Fix] Added missing import
import com.example.aicopilot.dto.chat.ChatResponse;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.definition.ProcessStep;
import com.example.aicopilot.dto.form.FormDefinitions;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.suggestion.AutoDiscoveryRequest;
import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import com.example.aicopilot.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Main controller for AI Copilot interactions.
 * All logic and responses are standardized in English.
 */
@Slf4j
@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final WorkflowOrchestrator orchestrator;
    private final JobRepository jobRepository;
    private final SuggestionAgent suggestionAgent;
    private final ProcessOutliner processOutliner;
    private final FlowAnalyst flowAnalyst;
    private final DataModeler dataModeler;
    private final FormUXDesigner formUXDesigner;
    private final DataContextService dataContextService;
    private final ChatLanguageModel chatLanguageModel;
    private final AssetRepository assetRepository;
    private final AssetAnalysisService assetAnalysisService;
    private final ObjectMapper objectMapper;

    /**
     * Integrated Chat Endpoint.
     * Triggers asynchronous process generation for every request.
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chatWithAi(@RequestBody ChatRequest request) {
        String prompt = request.userPrompt();

        // Initialize a new design job
        String jobId = UUID.randomUUID().toString();
        jobRepository.initJob(jobId);

        log.info("Starting Chat-Driven Design Job [{}]. User Prompt: '{}'", jobId, prompt);

        // Execute the orchestration logic asynchronously
        orchestrator.runChatJob(jobId, prompt, request.selectedAssetIds());

        // Return the Job ID for frontend polling
        return ResponseEntity.accepted().body(new ChatResponse(
                null,
                jobId
        ));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startJob(@RequestBody Map<String, String> request) {
        String prompt = request.get("userPrompt");
        String jobId = UUID.randomUUID().toString();
        jobRepository.initJob(jobId);
        orchestrator.runChatJob(jobId, prompt, java.util.List.of());
        return ResponseEntity.accepted().body(Map.of("jobId", jobId, "message", "Standard Job Started"));
    }

    @PostMapping("/transform")
    public ResponseEntity<?> transformJob(@RequestBody ProcessDefinition definition) {
        String jobId = UUID.randomUUID().toString();
        try {
            String definitionJson = objectMapper.writeValueAsString(definition);
            jobRepository.initJob(jobId);
            orchestrator.runTransformationJob(jobId, definitionJson);
            return ResponseEntity.accepted().body(Map.of("jobId", jobId, "message", "Transformation Job Started"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid Process Definition");
        }
    }

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

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeProcess(@RequestBody Map<String, Object> graphSnapshot) {
        try {
            if (flowAnalyst == null) {
                throw new IllegalStateException("FlowAnalyst bean is not initialized.");
            }

            Object nodesObj = graphSnapshot.get("nodes");
            Object edgesObj = graphSnapshot.get("edges");

            if (nodesObj == null || edgesObj == null) {
                throw new IllegalArgumentException("Nodes or Edges data is missing in the snapshot.");
            }

            String nodesJson = objectMapper.writeValueAsString(nodesObj);
            String edgesJson = objectMapper.writeValueAsString(edgesObj);

            AnalysisReport report = flowAnalyst.analyzeGraph(nodesJson, edgesJson);

            return ResponseEntity.ok(report.results());
        } catch (Exception e) {
            log.error("Analysis failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown analysis error"
            ));
        }
    }

    @PostMapping("/analyze/fix")
    public ResponseEntity<GraphStructure> fixError(@RequestBody FixGraphRequest request) {
        try {
            String graphJson = objectMapper.writeValueAsString(request.graphSnapshot());
            AnalysisResult error = request.error();

            GraphStructure fixedGraph = flowAnalyst.fixGraph(
                    graphJson,
                    error.type(),
                    error.targetNodeId(),
                    error.suggestion()
            );

            return ResponseEntity.ok(fixedGraph);
        } catch (Exception e) {
            log.error("Auto-fix failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/suggest/form")
    public ResponseEntity<FormDefinitions> suggestForm(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        FormDefinitions form = suggestionAgent.suggestFormDefinition(prompt);
        return ResponseEntity.ok(form);
    }

    @PostMapping("/suggest/data-model/auto-discovery")
    public ResponseEntity<DataEntitiesResponse> suggestMissingEntities(@RequestBody AutoDiscoveryRequest request) {
        try {
            String processJson = objectMapper.writeValueAsString(request.processContext());
            String dataJson = objectMapper.writeValueAsString(request.existingEntities());

            DataEntitiesResponse suggestions = dataModeler.suggestMissingEntities(processJson, dataJson);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Data auto-discovery failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/suggest/form/auto-discovery")
    public ResponseEntity<FormResponse> suggestMissingForms(@RequestBody Map<String, Object> request) {
        try {
            Object processContext = request.get("processContext");
            Object dataContext = request.get("existingEntities");
            Object existingForms = request.get("existingForms");

            String processJson = objectMapper.writeValueAsString(processContext);
            String dataJson = objectMapper.writeValueAsString(dataContext);
            String formsJson = objectMapper.writeValueAsString(existingForms);

            FormResponse suggestions = formUXDesigner.suggestMissingForms(processJson, dataJson, formsJson);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Form auto-discovery failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/analyze/asset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessDefinition> analyzeAsset(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ProcessDefinition response = assetAnalysisService.analyzeAssetToDefinition(file);
        return ResponseEntity.ok(response);
    }
}