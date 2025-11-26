package com.example.aicopilot.dto.process.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Unified Configuration Record supporting Smart Binding Protocol.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // [필수] 정의되지 않은 필드 무시
public record NodeConfiguration(
        @JsonProperty("configType")
        @JsonPropertyDescription("Discriminator: 'USER_TASK_CONFIG', 'EMAIL_CONFIG', 'GATEWAY_CONFIG'")
        String configType,

        // --- User Task & Approval Fields ---
        @JsonProperty("participantRole")
        @JsonPropertyDescription("Who performs this task? Can be a static role or a binding expression.")
        String participantRole,

        @JsonProperty("formKey")
        @JsonPropertyDescription("Link to a Form Definition. Used to infer required inputs.")
        String formKey,

        @JsonProperty("isApproval")
        @JsonPropertyDescription("If true, renders Approve/Reject buttons automatically.")
        Boolean isApproval,

        @JsonProperty("dueDuration")
        String dueDuration,

        // --- Service Task (Email/API) Fields ---
        @JsonProperty("templateId")
        String templateId,

        @JsonProperty("subject")
        @JsonPropertyDescription("Email subject. Can contain binding expressions like {{ node.var }}.")
        String subject,

        @JsonProperty("retryCount")
        Integer retryCount,

        @JsonProperty("priority")
        String priority,

        // --- Gateway Fields ---
        @JsonProperty("defaultNextActivityId")
        String defaultNextActivityId,

        @JsonProperty("conditions")
        List<BranchCondition> conditions
) {
        @JsonIgnoreProperties(ignoreUnknown = true) // [필수] 내부 레코드에도 적용
        public record BranchCondition(
                @JsonProperty("expression")
                @JsonPropertyDescription("JavaScript-like expression using variables (e.g., `{{ node.amount }} > 1000`).")
                String expression,

                @JsonProperty("targetActivityId")
                String targetActivityId
        ) {}
}