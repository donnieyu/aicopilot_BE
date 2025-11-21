package com.example.aicopilot.service;

import com.example.aicopilot.agent.DataModeler;
import com.example.aicopilot.agent.FormUXDesigner;
import com.example.aicopilot.agent.ProcessArchitect;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AI 에이전트들을 조율하여 전체 워크플로우 설계를 수행하는 오케스트레이터 서비스.
 * 비동기로 실행되며 각 단계별 상태를 JobRepository에 업데이트합니다.
 */
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
     *
     * @param jobId 작업 식별자
     * @param userRequest 사용자의 자연어 요구사항
     */
    @Async
    public void runAsyncJob(String jobId, String userRequest) {
        try {
            // ---------------------------------------------------------
            // 1단계: 프로세스 아키텍처 설계 (Process Architecture)
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "1/3단계: 프로세스 흐름 설계 중...");

            // 프로세스 설계 에이전트 호출
            ProcessResponse process = processArchitect.designProcess(userRequest);

            // 결과 저장 및 JSON 변환 (다음 단계 컨텍스트용)
            jobRepository.saveArtifact(jobId, "PROCESS", process);
            String processJson = objectMapper.writeValueAsString(process);


            // ---------------------------------------------------------
            // 2단계: 데이터 모델링 (Data Modeling)
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "2/3단계: 데이터 모델 정의 중...");

            // [변경] userRequest를 함께 전달하여 AI가 구체적인 필드(원자성)를 유추하도록 함
            DataEntitiesResponse data = dataModeler.designDataModel(userRequest, processJson);

            // 결과 저장 및 JSON 변환
            jobRepository.saveArtifact(jobId, "DATA", data);
            String dataJson = objectMapper.writeValueAsString(data);


            // ---------------------------------------------------------
            // 3단계: 폼 UX 디자인 (Form UX Design)
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "3/3단계: 화면(Form) 구성 및 권한 설정 중...");

            // 폼 디자인 에이전트 호출 (Process + Data 컨텍스트 주입)
            FormResponse form = formUXDesigner.designForm(userRequest, processJson, dataJson);

            // 최종 결과 저장 및 완료 처리
            jobRepository.saveArtifact(jobId, "FORM", form);
            jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "모든 설계가 완료되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 상태 업데이트
            jobRepository.updateState(jobId, JobStatus.State.FAILED, "작업 중 오류 발생: " + e.getMessage());
        }
    }
}