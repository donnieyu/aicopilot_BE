package com.example.aicopilot.service;

import com.example.aicopilot.agent.DataModeler;
import com.example.aicopilot.agent.FlowAnalyst;
import com.example.aicopilot.agent.FormUXDesigner;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.ProgressStep;
import com.example.aicopilot.dto.analysis.AnalysisReport;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.event.ProcessGeneratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * [Ver 11.2] 비동기 아티팩트 생성 및 최종 분석(Audit) 통합 로직.
 */
@Service
@RequiredArgsConstructor
public class AsyncArtifactGenerator {

    private final DataModeler dataModeler;
    private final FormUXDesigner formUXDesigner;
    private final FlowAnalyst flowAnalyst; // [New] 분석 에이전트 주입
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener
    public void handleProcessGenerated(ProcessGeneratedEvent event) {
        String jobId = event.getJobId();
        try {
            String userRequest = event.getUserRequest();
            String processJson = objectMapper.writeValueAsString(event.getProcessResponse());

            // 1. Data Modeling Step
            jobRepository.upsertProgressStep(jobId, "data", "Extracting data attributes", ProgressStep.Status.IN_PROGRESS);
            long startData = System.currentTimeMillis();
            DataEntitiesResponse data = dataModeler.designDataModel(userRequest, processJson);
            jobRepository.saveArtifact(jobId, "DATA", data, System.currentTimeMillis() - startData);
            jobRepository.upsertProgressStep(jobId, "data", "Data attributes extracted", ProgressStep.Status.COMPLETED);

            // 2. Form UX Design Step
            jobRepository.upsertProgressStep(jobId, "form", "Optimizing form layouts", ProgressStep.Status.IN_PROGRESS);
            String dataJson = objectMapper.writeValueAsString(data);
            long startForm = System.currentTimeMillis();
            FormResponse form = formUXDesigner.designForm(userRequest, processJson, dataJson);
            jobRepository.saveArtifact(jobId, "FORM", form, System.currentTimeMillis() - startForm);
            jobRepository.upsertProgressStep(jobId, "form", "Form layouts optimized", ProgressStep.Status.COMPLETED);

            // 3. Final Audit Step (Shadow Architect Integration)
            // [Fix] 설계 완료 후 항상 마지막 단계로 분석 로직 실행
            jobRepository.upsertProgressStep(jobId, "audit", "Auditing logical integrity", ProgressStep.Status.IN_PROGRESS);

            // 프로세스 맵의 노드/엣지 정보를 시각화 응답 객체로부터 추출 (Analysis API 호환 형식)
            String nodesJson = objectMapper.writeValueAsString(event.getProcessResponse().activities());
            // 엣지는 노드 내부의 nextActivityId 정보를 기반으로 FlowAnalyst가 내부적으로 판단하거나, 별도 매핑 필요
            // 여기서는 FlowAnalyst.analyzeGraph 인터페이스에 맞춰 변환 전송
            AnalysisReport report = flowAnalyst.analyzeGraph(processJson, "[]"); // Simplified for now

            if (report != null && report.results() != null) {
                jobRepository.saveAnalysisResults(jobId, report.results());
            }

            jobRepository.upsertProgressStep(jobId, "audit", "Logical integrity audited", ProgressStep.Status.COMPLETED);

            // Complete all tasks
            jobRepository.updateState(jobId, JobStatus.State.COMPLETED, "Architecture Completed Successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            jobRepository.updateState(jobId, JobStatus.State.FAILED, "Critical failure during analysis: " + e.getMessage());
            jobRepository.upsertProgressStep(jobId, "audit", "Audit failed", ProgressStep.Status.FAILED);
        }
    }
}