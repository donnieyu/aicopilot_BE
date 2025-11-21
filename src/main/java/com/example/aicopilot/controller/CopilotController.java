package com.example.aicopilot.controller;

import com.example.aicopilot.agent.SuggestionAgent;
import com.example.aicopilot.dto.JobStatus;
import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import com.example.aicopilot.service.JobRepository;
import com.example.aicopilot.service.WorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final WorkflowOrchestrator orchestrator;
    private final JobRepository jobRepository;
    private final SuggestionAgent suggestionAgent; // [NEW] 제안 에이전트 주입

    /**
     * 1. 작업 시작 (비동기)
     */
    @PostMapping("/start")
    public ResponseEntity<?> startJob(@RequestBody Map<String, String> request) {
        String prompt = request.get("userPrompt");
        String jobId = UUID.randomUUID().toString();
        jobRepository.initJob(jobId);
        orchestrator.runAsyncJob(jobId, prompt);

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "message", "작업이 백그라운드에서 시작되었습니다."
        ));
    }

    /**
     * 2. 상태 조회 (Polling)
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        JobStatus status = jobRepository.findById(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        String etag = "\"" + status.version() + "\"";
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(status);
    }

    /**
     * 3. [NEW] 실시간 AI 제안 요청 (On-Demand)
     * 사용자가 그래프 작성 중 특정 노드에서 "다음 단계 추천"을 요청할 때 사용합니다.
     *
     * Request Body:
     * {
     * "currentGraphJson": "{ ... }", // 현재 프론트엔드에 그려진 그래프 전체 (JSON String)
     * "focusNodeId": "node_submit_expense" // 기준이 되는 노드 ID
     * }
     */
    @PostMapping("/suggest")
    public ResponseEntity<SuggestionResponse> suggestNextSteps(@RequestBody Map<String, String> request) {
        String currentGraphJson = request.get("currentGraphJson");
        String focusNodeId = request.get("focusNodeId");

        // 프롬프트 구성
        String prompt = "Analyze the provided graph and suggest the next logical steps after node: " + focusNodeId;

        // AI 에이전트 호출 (동기 방식 - 사용자가 기다림)
        SuggestionResponse response = suggestionAgent.suggestNextSteps(prompt, currentGraphJson, focusNodeId);

        return ResponseEntity.ok(response);
    }
}