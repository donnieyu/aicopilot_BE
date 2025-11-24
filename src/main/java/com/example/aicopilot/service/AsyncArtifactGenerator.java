package com.example.aicopilot.service;

import com.example.aicopilot.agent.DataModeler;
import com.example.aicopilot.agent.FormUXDesigner;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.event.ProcessGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 비동기 산출물 생성기.
 * ProcessGeneratedEvent를 구독하여 데이터 모델링 및 폼 디자인을 백그라운드에서 수행합니다.
 */
@Service
@RequiredArgsConstructor
public class AsyncArtifactGenerator {

    private final DataModeler dataModeler;
    private final FormUXDesigner formUXDesigner;
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Async // 별도 스레드에서 실행 (메인 흐름 차단 방지)
    @EventListener
    public void handleProcessGenerated(ProcessGeneratedEvent event) {
        try {
            String jobId = event.getJobId();
            String userRequest = event.getUserRequest();
            String processJson = objectMapper.writeValueAsString(event.getProcessResponse());

            // ---------------------------------------------------------
            // 2단계: 데이터 모델링 (Data Modeling)
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "2/3단계: 데이터 모델 정의 중...");
            long startData = System.currentTimeMillis();

            // 데이터 모델러 호출 (User Request + Process Context)
            DataEntitiesResponse data = dataModeler.designDataModel(userRequest, processJson);

            long durationData = System.currentTimeMillis() - startData;
            jobRepository.saveArtifact(jobId, "DATA", data, durationData);

            // ---------------------------------------------------------
            // 3단계: 폼 UX 디자인 (Form UX Design)
            // ---------------------------------------------------------
            jobRepository.updateState(jobId, JobStatus.State.PROCESSING, "3/3단계: 화면(Form) 구성 및 권한 설정 중...");
            String dataJson = objectMapper.writeValueAsString(data);
            long startForm = System.currentTimeMillis();

            // 폼 디자이너 호출 (Process + Data Context)
            FormResponse form = formUXDesigner.designForm(userRequest, processJson, dataJson);

            long durationForm = System.currentTimeMillis() - startForm;
            jobRepository.saveArtifact(jobId, "FORM", form, durationForm);

            // 전체 완료 처리
            jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "모든 설계가 완료되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            // 에러 발생 시 상태 업데이트 (이미 프로세스는 성공했으므로 부분 실패 처리 고려 가능)
            jobRepository.updateState(event.getJobId(), JobStatus.State.FAILED, "후속 작업 중 오류: " + e.getMessage());
        }
    }
}