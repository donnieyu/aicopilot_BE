package com.example.aicopilot.service;

import com.example.aicopilot.dto.*;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 작업 상태 및 산출물을 관리하는 인메모리 저장소.
 * 단계별 소요 시간 및 진행 상태를 추적합니다.
 */
@Component
public class JobRepository {
    private final Map<String, JobStatus> store = new ConcurrentHashMap<>();

    public void save(JobStatus status) {
        store.put(status.jobId(), status);
    }

    public JobStatus findById(String jobId) {
        JobStatus status = store.get(jobId);
        if (status != null && status.state() == JobStatus.State.PROCESSING) {
            // 조회 시점의 실시간 경과 시간 계산 (사용자에게 흐르는 시간 표시용)
            long currentElapsed = System.currentTimeMillis() - status.startTime();
            return new JobStatus(
                    status.jobId(), status.state(), status.message(), status.lastUpdatedStage(),
                    status.version(), status.startTime(), status.stageDurations(), currentElapsed,
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
                    current.processResponse(), current.dataEntitiesResponse(), current.formResponse()
            ));
        }
    }

    // 아티팩트 저장 및 소요 시간 기록 메서드들
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
                    stageName, // 마지막 업데이트 단계 갱신
                    current.version() + 1,
                    current.startTime(),
                    newDurations,
                    elapsed,
                    p, d, f
            ));
        }
    }
}