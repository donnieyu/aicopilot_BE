package com.example.aicopilot.controller;

import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.service.JobRepository;
import com.example.aicopilot.service.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final WorkflowOrchestrator orchestrator;
    private final JobRepository jobRepository;

    /**
     * 1. 작업 시작 (비동기)
     * - 클라이언트는 이 API를 호출하고 jobId를 즉시 받습니다.
     */
    @PostMapping("/start")
    public ResponseEntity<?> startJob(@RequestBody Map<String, String> request) {
        String prompt = request.get("userPrompt");

        // [Step 1] Job ID 생성
        String jobId = UUID.randomUUID().toString();

        // [Step 2] 저장소 초기화 (동기) - Polling 시 404 방지
        jobRepository.initJob(jobId);

        // [Step 3] 비동기 작업 실행 (Fire-and-Forget)
        orchestrator.runAsyncJob(jobId, prompt);

        // [Step 4] 즉시 응답 (0.1초 소요)
        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "message", "작업이 백그라운드에서 시작되었습니다. 상태를 조회하세요."
        ));
    }

    /**
     * 2. 상태 조회 (Polling)
     * - 클라이언트는 1초 간격으로 이 API를 호출하여 진행 상황을 확인합니다.
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        JobStatus status = jobRepository.findById(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}