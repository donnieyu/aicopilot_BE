package com.example.aicopilot.dto.process.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * 통합 설정 레코드 (Unified Configuration Record).
 * 모든 노드 타입(UserTask, ServiceTask, Gateway)의 설정 필드를 하나의 구조로 병합했습니다.
 * 이는 AI 응답 파싱 시 인터페이스(Interface) 역직렬화 문제를 방지하기 위함입니다.
 */
public record NodeConfiguration(
        @JsonProperty("configType")
        @JsonPropertyDescription("Discriminator for config type: 'USER_TASK_CONFIG', 'EMAIL_CONFIG', 'GATEWAY_CONFIG'")
        String configType,

        // --- User Task Fields ---
        @JsonProperty("participantRole")
        @JsonPropertyDescription("Role responsible for the task (e.g., 'Initiator', 'Manager of Initiator').")
        String participantRole,

        @JsonProperty("formKey")
        @JsonPropertyDescription("Reference key for the UI Form schema.")
        String formKey,

        @JsonProperty("isApproval")
        @JsonPropertyDescription("Whether to automatically render Approve/Reject buttons.")
        Boolean isApproval,

        @JsonProperty("dueDuration")
        @JsonPropertyDescription("SLA duration in ISO-8601 format (e.g., 'P3D').")
        String dueDuration,

        // --- Email/Service Task Fields ---
        @JsonProperty("templateId")
        @JsonPropertyDescription("ID of the email template.")
        String templateId,

        @JsonProperty("subject")
        @JsonPropertyDescription("Email subject line.")
        String subject,

        @JsonProperty("retryCount")
        @JsonPropertyDescription("Number of retries on failure.")
        Integer retryCount,

        @JsonProperty("priority")
        @JsonPropertyDescription("Priority level (LOW, NORMAL, HIGH).")
        String priority,

        // --- Gateway Fields ---
        // defaultNextActivityId is effectively handled by Activity.nextActivityId but kept for schema completeness if needed.
        @JsonProperty("defaultNextActivityId")
        @JsonPropertyDescription("Fallback path if no conditions match (redundant with Activity.nextActivityId).")
        String defaultNextActivityId,

        @JsonProperty("conditions")
        @JsonPropertyDescription("List of branching conditions. If none match, flow proceeds to Activity.nextActivityId.")
        List<BranchCondition> conditions
) {
    /**
     * 게이트웨이 분기 조건 내부 레코드
     */
    public record BranchCondition(
            @JsonProperty("expression")
            @JsonPropertyDescription("Conditional expression (e.g., 'amount > 1000').")
            String expression,

            @JsonProperty("targetActivityId")
            @JsonPropertyDescription("Target Node ID to traverse to if condition is met.")
            String targetActivityId
    ) {}
}