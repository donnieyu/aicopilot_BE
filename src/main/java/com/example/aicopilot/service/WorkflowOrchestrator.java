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
 * 워크플로우 오케스트레이터 (Ver 7.1).
 * 2-Step Generation (Outliner -> Transformer) 및 Self-Correction 파이프라인을 관리합니다.
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
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "1단계: 요구사항을 분석하여 단계 리스트(Outliner) 작성 중...");
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

    // 공통 변환 및 검증 로직
    private void transformAndFinalize(String jobId, String userRequest, String definitionJson) throws Exception {
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "2단계: 리스트를 분석하여 BPMN 프로세스 맵으로 변환(Transformation) 중...");

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
                            String.format("구조적 오류 자동 수정 중... (시도 %d/%d)", attempt, maxRetries));

                    String invalidMapJson = objectMapper.writeValueAsString(process);
                    process = processArchitect.fixMap(definitionJson, invalidMapJson, lastError);
                }

                processValidator.validate(process);
                break;

            } catch (IllegalArgumentException e) {
                lastError = e.getMessage();
                if (attempt == maxRetries) throw new RuntimeException("프로세스 맵 변환 실패: " + lastError);
            }
        }

        long duration = System.currentTimeMillis() - startTransform;

        // [핵심 변경] 프로세스 생성 완료 시점에 즉시 아티팩트를 저장하고 상태를 업데이트합니다.
        // 프론트엔드는 이 시점에 Polling으로 프로세스 맵을 렌더링할 수 있습니다.
        jobRepository.saveArtifact(jobId, "PROCESS", process, duration);
        jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "프로세스 생성 완료! 데이터 모델링을 시작합니다..."); // 사용자에게 피드백 제공

        // 후속 작업(데이터, 폼)은 이벤트 발행을 통해 비동기로 계속 진행
        eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));
    }

    private void handleError(String jobId, Exception e) {
        e.printStackTrace();
        jobRepository.updateState(jobId, JobStatus.State.FAILED, "작업 중 오류 발생: " + e.getMessage());
    }
}