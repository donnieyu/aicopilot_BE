package com.example.aicopilot.service;

import com.example.aicopilot.agent.*;
import com.example.aicopilot.dto.JobStatus;
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
 * Workflow Orchestrator (Ver 10.3 - JSON Mode Compliance)
 * 모든 AI 에이전트 호출 시 "json" 키워드 포함 및 구조적 응답을 보장합니다.
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

    @Async
    public void runChatJob(String jobId, ChatRequest request) {
        String userPrompt = request.userPrompt();
        List<String> assetIds = request.selectedAssetIds();
        String currentProcessJson = request.currentProcessJson();

        try {
            log.info("Job [{}] started. Validating and classifying intent...", jobId);

            // 1. Validation Rail (Prompt includes "JSON")
            ValidationResult validation = inputGuardAgent.validate(userPrompt);

            if (validation.status() == ValidationResult.ValidationStatus.INVALID) {
                jobRepository.updateState(jobId, JobStatus.State.COMPLETED, validation.message());
                return;
            }

            if (validation.status() == ValidationResult.ValidationStatus.BRIDGE) {
                jobRepository.updateState(jobId, JobStatus.State.COMPLETED, validation.message());
                return;
            }

            // 2. Intent Classification (Updated to IntentResponse record)
            IntentResponse response = intentClassifier.classify(userPrompt);
            IntentType intent = response.intent();
            log.info("Job [{}] intent determined: {}", jobId, intent);

            // 3. Routing
            switch (intent) {
                case DESIGN -> executeDesignFlow(jobId, userPrompt, assetIds);
                case MODIFY -> {
                    if (currentProcessJson == null || currentProcessJson.isBlank()) {
                        jobRepository.updateState(jobId, JobStatus.State.FAILED,
                                "Modification failed: No process context provided for modification.");
                    } else {
                        executeModificationFlow(jobId, userPrompt, currentProcessJson);
                    }
                }
                case ANALYZE -> jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "Optimization audit complete.");
                default -> jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "How can I help you with your design?");
            }
        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    private void executeDesignFlow(String jobId, String userPrompt, List<String> assetIds) throws Exception {
        String ragContext = buildContextFromAssets(assetIds);
        String augmentedPrompt = String.format("""
            ### MISSION: Senior Business Process Architect
            Analyze requirements and draft a structured business process in **JSON** format.
            
            USER REQUEST: "%s"
            KNOWLEDGE: %s
            """, userPrompt, ragContext.isEmpty() ? "General standards." : ragContext);

        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "AI Architect is drafting process steps...");
        ProcessDefinition definition = processOutliner.draftDefinition(augmentedPrompt);

        if (definition.steps() == null || definition.steps().isEmpty()) {
            jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "Please provide more details for the process design.");
            return;
        }

        transformAndFinalize(jobId, userPrompt, objectMapper.writeValueAsString(definition));
    }

    private void executeModificationFlow(String jobId, String userPrompt, String currentProcessJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Applying modifications in **JSON** format...");
        long startTime = System.currentTimeMillis();

        ProcessResponse updatedProcess = partialModifier.modifyProcess(currentProcessJson, userPrompt);
        processValidator.validate(updatedProcess);

        jobRepository.saveArtifact(jobId, "PROCESS", updatedProcess, System.currentTimeMillis() - startTime);
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Modification applied successfully.");

        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userPrompt, updatedProcess));
    }

    public void runQuickStartJob(String jobId, String userRequest) {
        runChatJob(jobId, new ChatRequest(userRequest, List.of(), null));
    }

    public void runTransformationJob(String jobId, String definitionJson) {
        try {
            transformAndFinalize(jobId, "Manual Transformation", definitionJson);
        } catch (Exception e) {
            handleError(jobId, e);
        }
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

    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Generating visual BPMN diagram in **JSON**...");
        long startTransform = System.currentTimeMillis();
        ProcessResponse process = processArchitect.transformToMap(definitionJson);
        processValidator.validate(process);

        jobRepository.saveArtifact(jobId, "PROCESS", process, System.currentTimeMillis() - startTransform);
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Syncing data models and forms...");

        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private void handleError(String jobId, Exception e) {
        log.error("Job [{}] failed: {}", jobId, e.getMessage());
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "System error: " + e.getMessage());
    }
}