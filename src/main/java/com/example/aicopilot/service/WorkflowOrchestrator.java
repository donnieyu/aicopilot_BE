package com.example.aicopilot.service;

import com.example.aicopilot.agent.ProcessArchitect;
import com.example.aicopilot.agent.ProcessOutliner;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.event.ProcessGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Workflow Orchestrator (Ver 7.1).
 * Manages the 2-Step Generation (Outliner -> Transformer) and Self-Correction pipeline.
 */
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final ProcessOutliner processOutliner;
    private final ProcessArchitect processArchitect;
    private final ProcessValidator processValidator;
    private final JobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Mode A: Quick Start (Natural Language -> List -> Map)
     */
    @Async
    public void runQuickStartJob(String jobId, String userRequest) {
        try {
            // Step 1: Outlining
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Step 1: Analyzing requirements and drafting the step list (Outliner)...");
            ProcessDefinition definition = processOutliner.draftDefinition(userRequest);
            String definitionJson = objectMapper.writeValueAsString(definition);

            // Step 2: Transformation
            transformAndFinalize(jobId, userRequest, definitionJson);

        } catch (Exception e) {
            handleError(jobId, e);
        }
    }

    /**
     * Mode B: Transformation (List -> Map)
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

    // Common transformation and validation logic
    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Step 2: Transforming list into Process Map...");

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

        // [Key Change] Save artifact immediately upon process generation completion and update state.
        // Frontend can render the process map via Polling at this point.
        jobRepository.saveArtifact(jobId, "PROCESS", process, duration);
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "Process generation complete! Starting data modeling...");

        // Subsequent tasks (Data, Form) proceed asynchronously via event publishing
        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private void handleError(String jobId, Exception e) {
        e.printStackTrace();
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "Error occurred during operation: " + e.getMessage());
    }
}