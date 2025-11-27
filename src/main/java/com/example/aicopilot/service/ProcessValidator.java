package com.example.aicopilot.service;

import com.example.aicopilot.dto.process.Activity;
import com.example.aicopilot.dto.process.NodeType;
import com.example.aicopilot.dto.process.ProcessResponse;
import com.example.aicopilot.dto.process.config.NodeConfiguration;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Process Design Validator (The Inspector).
 * Checks if the AI-generated process definition is logically valid.
 * [Fix] Removed mandatory check for physical END_EVENT node.
 */
@Component
public class ProcessValidator {

    private static final String VIRTUAL_END_NODE = "node_end";

    public void validate(ProcessResponse process) {
        if (process.activities() == null || process.activities().isEmpty()) {
            throw new IllegalArgumentException("Process must have at least one activity.");
        }

        // 1. 모든 실제 노드 ID 수집 (Source of Truth)
        Set<String> validNodeIds = process.activities().stream()
                .map(Activity::id)
                .collect(Collectors.toSet());

        // 가상 종료 노드 ID 허용
        validNodeIds.add(VIRTUAL_END_NODE);

        for (Activity activity : process.activities()) {
            validateNextActivityId(activity, validNodeIds);
            validateGatewayConditions(activity, validNodeIds);
        }
    }

    private void validateNextActivityId(Activity activity, Set<String> validNodeIds) {
        String nextId = activity.nextActivityId();

        // Gateway가 아닌 일반 Task는 nextActivityId가 필수 (단, 마지막 노드일 경우 node_end를 가리켜야 함)
        if (activity.type() != NodeType.EXCLUSIVE_GATEWAY && nextId == null) {
            // AI가 실수로 null을 보냈더라도, 로직상 허용하거나 경고를 줄 수 있음.
            // 여기서는 Strict하게 가되, AI가 'node_end'를 잘 넣도록 유도.
            throw new IllegalArgumentException(String.format(
                    "흐름 단절 오류: 노드 ['%s'](Type: %s)에 다음 단계(nextActivityId)가 정의되지 않았습니다. 종료 지점이라면 'node_end'를 지정하세요.",
                    activity.id(), activity.type()
            ));
        }

        if (nextId != null && !validNodeIds.contains(nextId)) {
            throw new IllegalArgumentException(String.format(
                    "Structural Error Detected: Node ['%s'] refers to non-existent node ['%s'] as nextActivityId.",
                    activity.id(), nextId
            ));
        }
    }

    private void validateGatewayConditions(Activity activity, Set<String> validNodeIds) {
        NodeConfiguration config = activity.configuration();
        if (config != null && config.conditions() != null) {
            for (NodeConfiguration.BranchCondition condition : config.conditions()) {
                String targetId = condition.targetActivityId();
                if (targetId != null && !validNodeIds.contains(targetId)) {
                    throw new IllegalArgumentException(String.format(
                            "Structural Error Detected: Branch condition in Node ['%s'] refers to non-existent node ['%s'] as targetActivityId.",
                            activity.id(), targetId
                    ));
                }
            }
        }
    }
}