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
import java.util.stream.Collectors;

/**
 * Workflow Orchestrator (Updated for Chat-Driven Context)
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
     * [Phase 2] 지식 컨텍스트를 포함한 채팅 기반 프로세스 생성
     */
    @Async
    public void runChatJob(String jobId, String userPrompt, List<String> assetIds) {
        try {
            // 1. Context Construction
            String context = buildContextFromAssets(assetIds);

            // [Fix] JSON 포맷 강제 문구 추가 (OpenAI json_object 모드 요구사항)
            String augmentedPrompt = userPrompt + "\n\n" + context + "\n\nIMPORTANT: Provide the response in valid JSON format.";

            log.info("Starting Chat Job {}. Assets: {}", jobId, assetIds.size());

            // 2. Step 1: Outlining (with Context)
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Step 1: Analyzing context and drafting steps...");

            // 기존 Outliner는 prompt만 받으므로, 컨텍스트를 prompt에 녹여서 전달
            ProcessDefinition definition = processOutliner.draftDefinition(augmentedPrompt);
            String definitionJson = objectMapper.writeValueAsString(definition);

            // 3. Step 2: Transformation
            transformAndFinalize(jobId, userPrompt, definitionJson);

        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    private String buildContextFromAssets(List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("### Context Knowledge (MUST COMPLY):\n");

        for (String id : assetIds) {
            assetRepository.findById(id).ifPresent(asset -> {
                sb.append("- Source: ").append(asset.fileName()).append("\n");
                // [Fix] summary() -> description()으로 메서드명 수정 (DTO 리팩토링 반영)
                sb.append("  Content Summary: ").append(asset.description()).append("\n");
                // 필요한 경우 전체 텍스트 주입 (토큰 제한 고려 필요)
                // sb.append("  Full Text: ").append(asset.extractedText()).append("\n");
            });
        }
        sb.append("--------------------------------------------------\n");
        return sb.toString();
    }

    /**
     * Mode A: Quick Start (Legacy wrapper)
     */
    @Async
    public void runQuickStartJob(String jobId, String userRequest) {
        // [Fix] Quick Start에서도 JSON 포맷 강제
        String jsonPrompt = userRequest + "\n\nPlease provide the output in JSON format.";
        runChatJob(jobId, jsonPrompt, List.of());
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

    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Step 2: Transforming into Process Map...");

        long startTransform = System.currentTimeMillis();
        ProcessResponse process = null;
        String lastError = null;
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (attempt == 1) {
                    process = processArchitect.transformToMap(definitionJson);
                } else {
                    jobRepository.updateState(jobId, JobStatus.State.PROCESSING,
                            String.format("Auto-correcting structural errors... (Attempt %d/%d)", attempt, maxRetries));

                    String invalidMapJson = objectMapper.writeValueAsString(process);
                    process = processArchitect.fixMap(definitionJson, invalidMapJson, lastError);
                }

                processValidator.validate(process);
                break;

            } catch (IllegalArgumentException e) {
                lastError = e.getMessage();
                if (attempt == maxRetries) throw new RuntimeException("Failed to transform Process Map: " + lastError);
            }
        }

        long duration = System.currentTimeMillis() - startTransform;

        jobRepository.saveArtifact(jobId, "PROCESS", process, duration);
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Process generated. Designing Data & Forms...");

        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private void handleError(String jobId, Exception e) {
        e.printStackTrace();
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "Error: " + e.getMessage());
    }
}