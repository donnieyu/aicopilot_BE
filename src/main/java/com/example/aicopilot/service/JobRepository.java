package com.example.aicopilot.service;

import com.example.aicopilot.dto.*;
import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import com.example.aicopilot.dto.form.FormResponse;
import com.example.aicopilot.dto.process.ProcessResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobRepository {
    // 실제 운영 환경에서는 Redis나 DB를 사용해야 합니다.
    private final Map<String, JobStatus> store = new ConcurrentHashMap<>();

    public void save(JobStatus status) {
        store.put(status.jobId(), status);
    }

    public JobStatus findById(String jobId) {
        return store.get(jobId);
    }

    // 컨트롤러에서 초기화를 위해 호출하는 메서드
    public void initJob(String jobId) {
        save(JobStatus.init(jobId));
    }

    // 상태 및 메시지 업데이트 (결과물 유지)
    public void updateState(String jobId, JobStatus.State state, String message) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId,
                    state,
                    message,
                    current.processResponse(),
                    current.dataEntitiesResponse(),
                    current.formResponse()
            ));
        }
    }

    // 1단계 결과 저장: ProcessResponse
    public void saveArtifact(String jobId, String type, ProcessResponse processResponse) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId,
                    JobStatus.State.PROCESSING,
                    current.message(),
                    processResponse, // 업데이트
                    current.dataEntitiesResponse(),
                    current.formResponse()
            ));
        }
    }

    // 2단계 결과 저장: DataEntitiesResponse
    public void saveArtifact(String jobId, String type, DataEntitiesResponse dataEntitiesResponse) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId,
                    JobStatus.State.PROCESSING,
                    current.message(),
                    current.processResponse(),
                    dataEntitiesResponse, // 업데이트
                    current.formResponse()
            ));
        }
    }

    // 3단계 결과 저장: FormResponse
    public void saveArtifact(String jobId, String type, FormResponse formResponse) {
        JobStatus current = findById(jobId);
        if (current != null) {
            save(new JobStatus(
                    jobId,
                    JobStatus.State.PROCESSING,
                    current.message(),
                    current.processResponse(),
                    current.dataEntitiesResponse(),
                    formResponse // 업데이트
            ));
        }
    }
}