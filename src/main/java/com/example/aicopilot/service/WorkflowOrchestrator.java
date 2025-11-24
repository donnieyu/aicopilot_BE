package com.example.aicopilot.service;

import com.example.aicopilot.agent.ProcessArchitect;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.event.ProcessGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 워크플로우 오케스트레이터.
 * 사용자 요청을 받아 프로세스 설계를 시작하고, 완료 시 후속 작업을 위한 이벤트를 발행합니다.
 */
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final ProcessArchitect processArchitect;
    private final JobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행기

    /**
     * 비동기 작업 실행.
     * 1단계(프로세스)만 수행하고, 이후 단계는 이벤트를 통해 비동기로 위임합니다.
     */
    @Async
    public void runAsyncJob(String jobId, String userRequest) {
        try {
            // ---------------------------------------------------------
            // 1단계: 프로세스 아키텍처 설계 (Process Architecture)
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "1/3단계: 프로세스 흐름 설계 중...");
            long start = System.currentTimeMillis();

            // 프로세스 설계 에이전트 호출
            ProcessResponse process = processArchitect.designProcess(userRequest);

            long duration = System.currentTimeMillis() - start;
            jobRepository.saveArtifact(jobId, "PROCESS", process, duration);

            // [핵심 변경] 다음 단계 직접 호출하지 않고 이벤트 발행
            // AsyncArtifactGenerator가 이 이벤트를 받아 데이터와 폼을 생성합니다.
            eventPublisher.publishEvent(new ProcessGeneratedEvent(this, jobId, userRequest, process));

        } catch (Exception e) {
            e.printStackTrace();
            jobRepository.updateState(jobId, JobStatus.State.FAILED, "작업 중 오류 발생: " + e.getMessage());
        }
    }
}