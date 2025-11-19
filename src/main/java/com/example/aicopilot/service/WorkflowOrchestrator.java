package com.example.aicopilot.service;

import com.example.aicopilot.agent.DataModeler;
import com.example.aicopilot.agent.FormUXDesigner;
import com.example.aicopilot.agent.ProcessArchitect;
import com.example.aicopilot.dto.*;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final ProcessArchitect processArchitect;
    private final DataModeler dataModeler;
    private final FormUXDesigner formUXDesigner;
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    /**
     * 1. 백그라운드 작업 실행 (@Async)
     * - Controller는 이 메서드를 호출하고 즉시 리턴합니다 (Fire-and-Forget).
     * - 실제 로직은 별도 스레드에서 3단계로 실행됩니다.
     */
    @Async
    public void runAsyncJob(String jobId, String userRequest) {
        try {
            // ---------------------------------------------------------
            // Stage 1: Process Architecture
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "1/3단계: 프로세스 흐름 설계 중...");

            // Agent 호출
            ProcessResponse process = processArchitect.designProcess(userRequest);

            // 결과 저장 & 컨텍스트 준비
            jobRepository.saveArtifact(jobId, "PROCESS", process);
            String processJson = objectMapper.writeValueAsString(process);


            // ---------------------------------------------------------
            // Stage 2: Data Modeling
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "2/3단계: 데이터 모델 정의 중...");

            // Agent 호출 (이전 단계의 processJson을 컨텍스트로 주입)
            DataEntitiesResponse data = dataModeler.designDataModel(processJson);

            // 결과 저장 & 컨텍스트 준비
            jobRepository.saveArtifact(jobId, "DATA", data);
            String dataJson = objectMapper.writeValueAsString(data);


            // ---------------------------------------------------------
            // Stage 3: Form UX Design
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "3/3단계: 화면(Form) 구성 및 권한 설정 중...");

            // Agent 호출 (Process + Data 컨텍스트 모두 주입)
            FormResponse form = formUXDesigner.designForm(userRequest, processJson, dataJson);

            // 최종 결과 저장
            jobRepository.saveArtifact(jobId, "FORM", form);
            jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "모든 설계가 완료되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 상태 업데이트
            jobRepository.updateState(jobId, JobStatus.State.FAILED, "작업 중 오류 발생: " + e.getMessage());
        }
    }
}