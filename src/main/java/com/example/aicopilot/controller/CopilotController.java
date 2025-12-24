package com.example.aicopilot.controller;

import com.example.aicopilot.agent.*;
import com.example.aicopilot.dto.*;
import com.example.aicopilot.dto.analysis.*;
import com.example.aicopilot.dto.chat.ChatRequest;
import com.example.aicopilot.dto.chat.ChatResponse;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.form.FormDefinitions;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.suggestion.AutoDiscoveryRequest;
import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import com.example.aicopilot.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AI Copilot 메인 컨트롤러.
 * WorkflowOrchestrator Ver 11.1의 동적 진행 단계(Progress Steps) 아키텍처에 맞춰 동기화되었습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final WorkflowOrchestrator orchestrator;
    private final JobRepository jobRepository;
    private final SuggestionAgent suggestionAgent;
    private final FlowAnalyst flowAnalyst;
    private final DataModeler dataModeler;
    private final FormUXDesigner formUXDesigner;
    private final DataContextService dataContextService;
    private final AssetAnalysisService assetAnalysisService;
    private final ObjectMapper objectMapper;

    /**
     * 지능형 채팅 엔드포인트.
     * 의도 분석 및 가드레일 검증을 거쳐 적절한 에이전트를 비동기로 할당합니다.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatWithAi(@RequestBody ChatRequest request) {
        String jobId = UUID.randomUUID().toString();

        // 작업 초기화 (Progress Steps 리스트가 빈 상태로 시작)
        jobRepository.initJob(jobId);

        log.info("Starting AI Chat Job [{}]. User Prompt: '{}'", jobId, request.userPrompt());

        // 비동기 오케스트레이션 실행
        orchestrator.runChatJob(jobId, request);

        // 즉시 Job ID 반환하여 프론트엔드가 폴링을 시작할 수 있게 함
        return ResponseEntity.accepted().body(new ChatResponse(
                null,
                jobId
        ));
    }

    /**
     * 자연어 기반 퀵 스타트 엔드포인트 (Legacy Support).
     * [Refactor] 오케스트레이터의 퀵스타트 전용 메서드를 호출합니다.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startJob(@RequestBody Map<String, String> request) {
        String prompt = request.get("userPrompt");
        String jobId = UUID.randomUUID().toString();
        jobRepository.initJob(jobId);

        orchestrator.runQuickStartJob(jobId, prompt);

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "message", "Standard Design Job Started"
        ));
    }

    /**
     * 정형화된 리스트 데이터를 BPMN 맵으로 변환하는 엔드포인트 (Mode B).
     * [Fix] 의도 분석을 건너뛰고 즉시 변환 단계로 진입하는 로직을 호출합니다.
     */
    @PostMapping("/transform")
    public ResponseEntity<?> transformJob(@RequestBody ProcessDefinition definition) {
        String jobId = UUID.randomUUID().toString();
        try {
            String definitionJson = objectMapper.writeValueAsString(definition);
            jobRepository.initJob(jobId);

            // 오케스트레이터의 직접 변환 메서드 호출
            orchestrator.runTransformationJob(jobId, definitionJson);

            return ResponseEntity.accepted().body(Map.of(
                    "jobId", jobId,
                    "message", "Direct Transformation Job Started"
            ));
        } catch (Exception e) {
            log.error("Transform request failed", e);
            return ResponseEntity.badRequest().body("Invalid Process Definition JSON format.");
        }
    }

    /**
     * 작업의 현재 상태(진행 단계, 결과물)를 조회합니다.
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatus> getStatus(@PathVariable String jobId) {
        JobStatus status = jobRepository.findById(jobId);
        if (status == null) return ResponseEntity.notFound().build();

        String etag = "\"" + status.version() + "\"";
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(status);
    }

    /**
     * 캔버스 내 노드 선택 시 AI 제안 엔드포인트.
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

    /**
     * 프로세스 논리 구조 분석 (Shadow Architect).
     */
    @PostMapping("/analyze")
    public ResponseEntity<List<AnalysisResult>> analyzeProcess(@RequestBody Map<String, Object> graphSnapshot) {
        try {
            String nodesJson = objectMapper.writeValueAsString(graphSnapshot.get("nodes"));
            String edgesJson = objectMapper.writeValueAsString(graphSnapshot.get("edges"));

            AnalysisReport report = flowAnalyst.analyzeGraph(nodesJson, edgesJson);
            return ResponseEntity.ok(report.results());
        } catch (Exception e) {
            log.error("Structural analysis failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * AI 분석 결과에 따른 자동 수정 시뮬레이션.
     */
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
            log.error("Auto-fix simulation failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 자연어 요청 기반 폼 설계 제안.
     */
    @PostMapping("/suggest/form")
    public ResponseEntity<FormDefinitions> suggestForm(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        FormDefinitions form = suggestionAgent.suggestFormDefinition(prompt);
        return ResponseEntity.ok(form);
    }

    /**
     * 프로세스-데이터 갭 분석 및 엔티티 자동 발견.
     */
    @PostMapping("/suggest/data-model/auto-discovery")
    public ResponseEntity<DataEntitiesResponse> suggestMissingEntities(@RequestBody AutoDiscoveryRequest request) {
        try {
            String processJson = objectMapper.writeValueAsString(request.processContext());
            String dataJson = objectMapper.writeValueAsString(request.existingEntities());

            DataEntitiesResponse suggestions = dataModeler.suggestMissingEntities(processJson, dataJson);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Data entity discovery failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 프로세스-폼 갭 분석 및 입력 UI 자동 발견.
     */
    @PostMapping("/suggest/form/auto-discovery")
    public ResponseEntity<FormResponse> suggestMissingForms(@RequestBody Map<String, Object> request) {
        try {
            String processJson = objectMapper.writeValueAsString(request.get("processContext"));
            String dataJson = objectMapper.writeValueAsString(request.get("existingEntities"));
            String formsJson = objectMapper.writeValueAsString(request.get("existingForms"));

            FormResponse suggestions = formUXDesigner.suggestMissingForms(processJson, dataJson, formsJson);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Form UI discovery failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 업로드된 자산 분석을 통한 프로세스 정의 도출.
     */
    @PostMapping(value = "/analyze/asset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessDefinition> analyzeAsset(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ProcessDefinition response = assetAnalysisService.analyzeAssetToDefinition(file);
        return ResponseEntity.ok(response);
    }
}