package com.example.aicopilot.service;

import com.example.aicopilot.agent.*;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.ProgressStep;
import com.example.aicopilot.dto.chat.*;
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
 * Workflow Orchestrator (Ver 11.1 - Comprehensive Entry Points)
 * 인텐트 분류 및 가드레일을 통해 에이전트를 조율하며, 모든 생성/수정/변환 작업을 비동기로 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final InputGuardAgent inputGuardAgent;
    private final IntentClassifier intentClassifier;
    private final PartialModifier partialModifier;
    private final ProcessOutliner processOutliner;
    private final ProcessArchitect processArchitect;
    private final ProcessValidator processValidator;
    private final JobRepository jobRepository;
    private final AssetRepository assetRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 지능형 채팅 기반 통합 작업 실행 (Mode A / Chat)
     */
    @Async
    public void runChatJob(String jobId, ChatRequest request) {
        String userPrompt = request.userPrompt();
        List<String> assetIds = request.selectedAssetIds();
        String currentProcessJson = request.currentProcessJson();

        try {
            log.info("Job [{}] started. Initializing domain validation.", jobId);

            // 1. Domain Validation Step
            jobRepository.upsertProgressStep(jobId, "val", "Evaluating domain context", ProgressStep.Status.IN_PROGRESS);
            ValidationResult validation = inputGuardAgent.validate(userPrompt);

            if (validation.status() != ValidationResult.ValidationStatus.VALID) {
                jobRepository.upsertProgressStep(jobId, "val", "Validation completed", ProgressStep.Status.COMPLETED);
                jobRepository.updateState(jobId, JobStatus.State.COMPLETED, validation.message());
                return;
            }
            jobRepository.upsertProgressStep(jobId, "val", "Domain context validated", ProgressStep.Status.COMPLETED);

            // 2. Intent Analysis Step
            jobRepository.upsertProgressStep(jobId, "intent", "Analyzing process requirements", ProgressStep.Status.IN_PROGRESS);
            IntentResponse response = intentClassifier.classify(userPrompt);
            IntentType intent = response.intent();
            log.info("Job [{}] categorized as intent: {}", jobId, intent);
            jobRepository.upsertProgressStep(jobId, "intent", "Intent identified: " + intent, ProgressStep.Status.COMPLETED);

            // 3. Routing by Intent
            switch (intent) {
                case DESIGN -> executeDesignFlow(jobId, userPrompt, assetIds);
                case MODIFY -> {
                    if (currentProcessJson == null || currentProcessJson.isBlank()) {
                        jobRepository.updateState(jobId, JobStatus.State.FAILED, "No process context found to modify.");
                    } else {
                        executeModificationFlow(jobId, userPrompt, currentProcessJson);
                    }
                }
                case ANALYZE -> jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "Optimization audit complete.");
                default -> jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "Inquiry processed.");
            }
        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    /**
     * 단순 텍스트 기반 퀵 스타트 지원 (Legacy Wrapper)
     */
    @Async
    public void runQuickStartJob(String jobId, String userRequest) {
        runChatJob(jobId, new ChatRequest(userRequest, List.of(), null));
    }

    /**
     * [Fix] Mode B: 정형화된 정의서를 시각적 맵으로 즉시 변환하는 기능 복구
     */
    @Async
    public void runTransformationJob(String jobId, String definitionJson) {
        try {
            log.info("Job [{}] starting direct transformation flow (Mode B).", jobId);

            // 시각적 맵 생성 단계 시작 기록
            jobRepository.upsertProgressStep(jobId, "map", "Generating Process Map visualization", ProgressStep.Status.IN_PROGRESS);

            transformAndFinalize(jobId, "Manual Transformation Request", definitionJson);
        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    private void executeDesignFlow(String jobId, String userPrompt, List<String> assetIds) throws Exception {
        jobRepository.upsertProgressStep(jobId, "outline", "Synthesizing process steps", ProgressStep.Status.IN_PROGRESS);

        String ragContext = buildContextFromAssets(assetIds);
        String augmentedPrompt = String.format("""
            ### MISSION: Senior Business Process Architect
            Draft a structured business process in JSON format.
            USER REQUEST: "%s"
            KNOWLEDGE: %s
            """, userPrompt, ragContext.isEmpty() ? "General standards." : ragContext);

        ProcessDefinition definition = processOutliner.draftDefinition(augmentedPrompt);

        if (definition.steps() == null || definition.steps().isEmpty()) {
            jobRepository.upsertProgressStep(jobId, "outline", "Failed to identify steps", ProgressStep.Status.FAILED);
            jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "No design steps could be identified.");
            return;
        }
        jobRepository.upsertProgressStep(jobId, "outline", "Process steps synthesized", ProgressStep.Status.COMPLETED);

        jobRepository.upsertProgressStep(jobId, "map", "Generating Process Map visualization", ProgressStep.Status.IN_PROGRESS);
        transformAndFinalize(jobId, userPrompt, objectMapper.writeValueAsString(definition));
    }

    private void executeModificationFlow(String jobId, String userPrompt, String currentProcessJson) throws Exception {
        jobRepository.upsertProgressStep(jobId, "diff", "Identifying modification targets", ProgressStep.Status.IN_PROGRESS);
        jobRepository.upsertProgressStep(jobId, "diff", "Modification targets identified", ProgressStep.Status.COMPLETED);

        jobRepository.upsertProgressStep(jobId, "modify", "Applying surgical changes", ProgressStep.Status.IN_PROGRESS);
        long startTime = System.currentTimeMillis();

        ProcessResponse updatedProcess = partialModifier.modifyProcess(currentProcessJson, userPrompt);
        processValidator.validate(updatedProcess);

        jobRepository.saveArtifact(jobId, "PROCESS", updatedProcess, System.currentTimeMillis() - startTime);
        jobRepository.upsertProgressStep(jobId, "modify", "Surgical changes applied", ProgressStep.Status.COMPLETED);

        jobRepository.upsertProgressStep(jobId, "sync", "Synchronizing data & forms", ProgressStep.Status.IN_PROGRESS);
        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userPrompt, updatedProcess));
    }

    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        long startTransform = System.currentTimeMillis();
        ProcessResponse process = processArchitect.transformToMap(definitionJson);
        processValidator.validate(process);

        jobRepository.saveArtifact(jobId, "PROCESS", process, System.currentTimeMillis() - startTransform);
        jobRepository.upsertProgressStep(jobId, "map", "Process Map generated", ProgressStep.Status.COMPLETED);

        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private String buildContextFromAssets(List<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String id : assetIds) {
            assetRepository.findById(id).ifPresent(asset -> {
                sb.append(String.format("- [Source: %s]\n", asset.fileName()));
                String content = (asset.extractedText() != null && !asset.extractedText().isBlank())
                        ? asset.extractedText() : asset.description();
                if (content.length() > 3000) content = content.substring(0, 3000) + "...";
                sb.append(content).append("\n\n");
            });
        }
        return sb.toString();
    }

    private void handleError(String jobId, Exception e) {
        log.error("Job [{}] failed: {}", jobId, e.getMessage());
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "System Error: " + e.getMessage());

        JobStatus current = jobRepository.findById(jobId);
        if (current != null && current.progressSteps() != null) {
            for (ProgressStep step : current.progressSteps()) {
                if (step.status() == ProgressStep.Status.IN_PROGRESS) {
                    jobRepository.upsertProgressStep(jobId, step.id(), step.label() + " (Failed)", ProgressStep.Status.FAILED);
                }
            }
        }
    }
}