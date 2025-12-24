package com.example.aicopilot.service;

import com.example.aicopilot.dto.*;
import com.example.aicopilot.dto.analysis.AnalysisResult;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobRepository {
    private final Map<String, JobStatus> store = new ConcurrentHashMap<>();

    public void save(JobStatus status) {
        store.put(status.jobId(), status);
    }

    public JobStatus findById(String jobId) {
        JobStatus status = store.get(jobId);
        if (status != null && status.state() == JobStatus.State.PROCESSING) {
            long currentElapsed = System.currentTimeMillis() - status.startTime();
            return new JobStatus(
                    status.jobId(), status.state(), status.message(), status.lastUpdatedStage(),
                    status.version(), status.startTime(), status.stageDurations(), currentElapsed,
                    status.progressSteps(), status.analysisResults(), // 결과 추가
                    status.processResponse(), status.dataEntitiesResponse(), status.formResponse()
            );
        }
        return status;
    }

    public void initJob(String jobId) {
        save(JobStatus.init(jobId));
    }

    public void updateState(String jobId, JobStatus.State state, String message) {
        JobStatus current = store.get(jobId);
        if (current != null) {
            long elapsed = System.currentTimeMillis() - current.startTime();
            save(new JobStatus(
                    jobId, state, message,
                    current.lastUpdatedStage(),
                    current.version() + 1,
                    current.startTime(),
                    current.stageDurations(),
                    elapsed,
                    current.progressSteps(),
                    current.analysisResults(),
                    current.processResponse(), current.dataEntitiesResponse(), current.formResponse()
            ));
        }
    }

    public void upsertProgressStep(String jobId, String stepId, String label, ProgressStep.Status status) {
        JobStatus current = store.get(jobId);
        if (current != null) {
            List<ProgressStep> steps = new ArrayList<>(current.progressSteps());
            boolean found = false;
            for (int i = 0; i < steps.size(); i++) {
                if (steps.get(i).id().equals(stepId)) {
                    steps.set(i, steps.get(i).withStatus(status));
                    found = true;
                    break;
                }
            }
            if (!found) steps.add(new ProgressStep(stepId, label, status));

            save(new JobStatus(
                    jobId, current.state(), current.message(),
                    current.lastUpdatedStage(),
                    current.version() + 1,
                    current.startTime(),
                    current.stageDurations(),
                    System.currentTimeMillis() - current.startTime(),
                    steps,
                    current.analysisResults(),
                    current.processResponse(), current.dataEntitiesResponse(), current.formResponse()
            ));
        }
    }

    /**
     * [New] 분석 결과(Audit Results)를 작업 상태에 저장합니다.
     */
    public void saveAnalysisResults(String jobId, List<AnalysisResult> results) {
        JobStatus current = store.get(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId, current.state(), current.message(),
                    current.lastUpdatedStage(),
                    current.version() + 1,
                    current.startTime(),
                    current.stageDurations(),
                    System.currentTimeMillis() - current.startTime(),
                    current.progressSteps(),
                    results, // 분석 결과 반영
                    current.processResponse(), current.dataEntitiesResponse(), current.formResponse()
            ));
        }
    }

    public void saveArtifact(String jobId, String type, ProcessResponse processResponse, long durationMillis) {
        updateArtifactWithDuration(jobId, type, processResponse, null, null, durationMillis);
    }

    public void saveArtifact(String jobId, String type, DataEntitiesResponse dataEntitiesResponse, long durationMillis) {
        updateArtifactWithDuration(jobId, type, null, dataEntitiesResponse, null, durationMillis);
    }

    public void saveArtifact(String jobId, String type, FormResponse formResponse, long durationMillis) {
        updateArtifactWithDuration(jobId, type, null, null, formResponse, durationMillis);
    }

    private void updateArtifactWithDuration(String jobId, String stageName,
                                            ProcessResponse proc, DataEntitiesResponse data, FormResponse form,
                                            long durationMillis) {
        JobStatus current = store.get(jobId);
        if (current != null) {
            Map<String, Long> newDurations = new HashMap<>(current.stageDurations());
            newDurations.put(stageName, durationMillis);
            long elapsed = System.currentTimeMillis() - current.startTime();
            ProcessResponse p = proc != null ? proc : current.processResponse();
            DataEntitiesResponse d = data != null ? data : current.dataEntitiesResponse();
            FormResponse f = form != null ? form : current.formResponse();

            save(new JobStatus(
                    jobId, JobStatus.State.PROCESSING, current.message(),
                    stageName,
                    current.version() + 1,
                    current.startTime(),
                    newDurations,
                    elapsed,
                    current.progressSteps(),
                    current.analysisResults(),
                    p, d, f
            ));
        }
    }
}