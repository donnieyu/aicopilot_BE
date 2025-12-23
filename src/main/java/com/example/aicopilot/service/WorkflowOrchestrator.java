package com.example.aicopilot.service;

import com.example.aicopilot.agent.ProcessArchitect;
import com.example.aicopilot.agent.ProcessOutliner;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.asset.Asset;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.event.ProcessGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Workflow Orchestrator (Ver 8.3 - Phase 3 Refinement)
 * AI가 사용자 의도를 파악하고, Context(Asset)를 기반으로 프로세스를 생성하는 핵심 엔진입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final ProcessOutliner processOutliner;
    private final ProcessArchitect processArchitect;
    private final ProcessValidator processValidator;
    private final JobRepository jobRepository;
    private final AssetRepository assetRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * [Phase 3] RAG 기반 채팅형 프로세스 생성 오케스트레이터
     * 사용자의 자연어 요청과 지식 자산(Asset)을 결합하여 프로세스 정의를 도출합니다.
     */
    @Async
    public void runChatJob(String jobId, String userPrompt, List<String> assetIds) {
        try {
            log.info("Starting Chat-Driven Job [{}]. Assets: {}", jobId, assetIds.size());

            // 1. Context Build (RAG)
            String ragContext = buildContextFromAssets(assetIds);

            // 2. Prompt Engineering (Chain of Thought & Guardrails)
            // AI에게 역할을 부여하고, 지식 기반으로만 답변하도록 제약을 겁니다.
            String augmentedPrompt = String.format("""
                ### 1. MISSION
                You are an expert 'Business Process Architect'.
                Analyze the user's request and the provided [Context Knowledge] to draft a structured business process.
                
                ### 2. CONTEXT KNOWLEDGE (Strict Reference)
                %s
                
                ### 3. USER REQUEST
                "%s"
                
                ### 4. INSTRUCTIONS (Critical)
                - **Fact-Based:** Design the process logic primarily based on the [Context Knowledge]. If the knowledge contradicts standard practices, follow the knowledge (company rules).
                - **Completeness:** Ensure the process has a clear start, logical steps, decision points (gateways), and an end.
                - **Role Assignment:** Infer precise roles (e.g., 'Team Lead', 'Finance Mgr') mentioned in the context.
                - **Output Format:** You MUST return a valid JSON object matching the `ProcessDefinition` schema (topic, steps[]).
                """,
                    ragContext.isEmpty() ? "(No specific knowledge provided. Use industry standards.)" : ragContext,
                    userPrompt
            );

            // 3. Step 1: Outlining (Drafting)
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "AI Architect is analyzing context & drafting steps...");

            // ProcessOutliner 에이전트를 호출하여 1차 구조(리스트)를 잡습니다.
            ProcessDefinition definition = processOutliner.draftDefinition(augmentedPrompt);
            String definitionJson = objectMapper.writeValueAsString(definition);

            log.debug("Draft generated for Job {}: {}", jobId, definitionJson);

            // 4. Step 2: Transformation (BPMN Map Generation)
            // 리스트를 그래프(Node/Edge)로 변환합니다.
            transformAndFinalize(jobId, userPrompt, definitionJson);

        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    /**
     * Asset ID 목록을 받아 텍스트 컨텍스트를 조립합니다.
     * 토큰 제한을 고려하여 요약(description) 위주로 구성하되, 필요시 원문(extractedText)을 포함할 수 있습니다.
     */
    private String buildContextFromAssets(List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (String id : assetIds) {
            assetRepository.findById(id).ifPresent(asset -> {
                sb.append(String.format("- [Source: %s]\n", asset.fileName()));
                // 원문 텍스트가 있으면 원문을 우선 사용 (정확도 향상), 없으면 요약본 사용
                String content = (asset.extractedText() != null && !asset.extractedText().isBlank())
                        ? asset.extractedText()
                        : asset.description();

                // 너무 긴 텍스트는 잘라서 넣는 로직 (간이 구현)
                if (content.length() > 3000) {
                    content = content.substring(0, 3000) + "...(truncated)";
                }
                sb.append(content).append("\n\n");
            });
        }
        return sb.toString();
    }

    /**
     * Mode A: Quick Start (Legacy wrapper)
     */
    @Async
    public void runQuickStartJob(String jobId, String userRequest) {
        // 기존 Quick Start도 이제 Chat Job 로직을 재활용하여 일관성 유지
        runChatJob(jobId, userRequest, List.of());
    }

    /**
     * Mode B: Transformation (Legacy)
     */
    @Async
    public void runTransformationJob(String jobId, String definitionJson) {
        try {
            String userRequest = "Manual Definition Transformation";
            transformAndFinalize(jobId, userRequest, definitionJson);
        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    // 공통 변환 및 검증 로직 (2-Pass Self-Correction 포함)
    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Structuring BPMN diagram...");

        long startTransform = System.currentTimeMillis();
        ProcessResponse process = null;
        String lastError = null;
        int maxRetries = 2; // 재시도 횟수 조정

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (attempt == 1) {
                    process = processArchitect.transformToMap(definitionJson);
                } else {
                    // 자가 수정 모드: 이전 에러 메시지를 AI에게 전달하여 수정 요청
                    jobRepository.updateState(jobId, JobStatus.State.PROCESSING,
                            String.format("Optimizing logic structure... (Pass %d)", attempt));

                    String invalidMapJson = objectMapper.writeValueAsString(process);
                    process = processArchitect.fixMap(definitionJson, invalidMapJson, lastError);
                }

                // 구조적 유효성 검증 (필수 연결, 고립 노드 체크 등)
                processValidator.validate(process);
                break; // 성공 시 루프 탈출

            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Transformation attempt {} failed: {}", attempt, lastError);

                if (attempt == maxRetries) {
                    // 마지막 시도도 실패하면 예외 전파 (사용자에게 알림)
                    throw new RuntimeException("Failed to generate valid process map: " + lastError);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTransform;

        // 결과 저장 및 이벤트 발행 (-> Data/Form 생성 트리거)
        jobRepository.saveArtifact(jobId, "PROCESS", process, duration);
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Map generated. Analyzing data requirements...");

        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private void handleError(String jobId, Exception e) {
        log.error("Job {} failed", jobId, e);
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "Processing Error: " + e.getMessage());
    }
}