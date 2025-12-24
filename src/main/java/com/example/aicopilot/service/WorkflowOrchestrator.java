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
 * Workflow Orchestrator (Ver 8.5 - Reintegrated Critical Instructions)
 * Manages the generation lifecycle with flexible intent detection and strict architectural rules.
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
     * Intelligent RAG Chat Job
     * Analyzes intent and applies strict architectural instructions based on context knowledge.
     */
    @Async
    public void runChatJob(String jobId, String userPrompt, List<String> assetIds) {
        try {
            log.info("Executing Chat-Driven Job [{}].", jobId);

            String ragContext = buildContextFromAssets(assetIds);

            // [Refinement] Reintegrated critical instructions into augmentedPrompt
            String augmentedPrompt = String.format("""
                ### MISSION
                You are a 'Senior Business Process Architect'.
                Analyze the user's request and the provided [Context Knowledge].
                
                ### USER REQUEST
                "%s"
                
                ### CONTEXT KNOWLEDGE
                %s
                
                ### INSTRUCTIONS (Critical)
                1. **Intent Detection:** If the user request is a simple greeting (e.g., 'Hi', 'Hello') or unrelated to business processes, JUST answer naturally and return an empty 'steps' array in the JSON.
                2. **Fact-Based:** Design the process logic primarily based on the [Context Knowledge]. If the knowledge contradicts standard practices, follow the knowledge (company rules).
                3. **Completeness:** Ensure the process has a clear start, logical steps, decision points (gateways), and an end.
                4. **Role Assignment:** Infer precise roles (e.g., 'Team Lead', 'Finance Mgr') mentioned in the context.
                5. **Output Format:** You MUST always return a valid JSON object matching the `ProcessDefinition` schema (topic, steps[]).
                """,
                    userPrompt,
                    ragContext.isEmpty() ? "No specific knowledge provided." : ragContext
            );

            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "AI Architect is evaluating your request...");

            // Step 1: Outlining (Drafting)
            ProcessDefinition definition = processOutliner.draftDefinition(augmentedPrompt);

            // If AI decided there are no steps to design (not a process request)
            if (definition.steps() == null || definition.steps().isEmpty()) {
                jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "AI has responded to your inquiry.");
                log.info("Job {} completed as a simple conversation (no process generated).", jobId);
                return;
            }

            String definitionJson = objectMapper.writeValueAsString(definition);

            // Step 2: Transformation (Mapping)
            transformAndFinalize(jobId, userPrompt, definitionJson);

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
                        ? asset.extractedText()
                        : asset.description();
                if (content.length() > 3000) content = content.substring(0, 3000) + "...";
                sb.append(content).append("\n\n");
            });
        }
        return sb.toString();
    }

    @Async
    public void runQuickStartJob(String jobId, String userRequest) {
        runChatJob(jobId, userRequest, List.of());
    }

    @Async
    public void runTransformationJob(String jobId, String definitionJson) {
        try {
            transformAndFinalize(jobId, "Manual Transformation", definitionJson);
        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Converting logic to BPMN map...");

        long startTransform = System.currentTimeMillis();
        ProcessResponse process = null;
        String lastError = null;
        int maxRetries = 2;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (attempt == 1) {
                    process = processArchitect.transformToMap(definitionJson);
                } else {
                    String invalidMapJson = objectMapper.writeValueAsString(process);
                    process = processArchitect.fixMap(definitionJson, invalidMapJson, lastError);
                }
                processValidator.validate(process);
                break;
            } catch (Exception e) {
                lastError = e.getMessage();
                if (attempt == maxRetries) throw new RuntimeException("Map structure error: " + lastError);
            }
        }

        jobRepository.saveArtifact(jobId, "PROCESS", process, System.currentTimeMillis() - startTransform);
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Drafting data models and forms...");

        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private void handleError(String jobId, Exception e) {
        log.error("Job {} failed: {}", jobId, e.getMessage());
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "System Error: " + e.getMessage());
    }
}