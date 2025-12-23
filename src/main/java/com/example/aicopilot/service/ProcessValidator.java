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
 */
@Component
public class ProcessValidator {

    private static final String VIRTUAL_END_NODE = "node_end";

    public void validate(ProcessResponse process) {
        if (process.activities() == null || process.activities().isEmpty()) {
            throw new IllegalArgumentException("Process must have at least one activity.");
        }

        Set<String> validNodeIds = process.activities().stream()
                .map(Activity::id)
                .collect(Collectors.toSet());

        validNodeIds.add(VIRTUAL_END_NODE);

        for (Activity activity : process.activities()) {
            validateNextActivityId(activity, validNodeIds);
            validateGatewayConditions(activity, validNodeIds);
        }
    }

    private void validateNextActivityId(Activity activity, Set<String> validNodeIds) {
        String nextId = activity.nextActivityId();

        if (activity.type() != NodeType.EXCLUSIVE_GATEWAY && nextId == null) {
            // [Fixed] Error message converted to English
            throw new IllegalArgumentException(String.format(
                    "Flow Disconnect: Node ['%s'](Type: %s) has no nextActivityId defined. If it's a terminal point, specify 'node_end'.",
                    activity.id(), activity.type()
            ));
        }

        if (nextId != null && !validNodeIds.contains(nextId)) {
            throw new IllegalArgumentException(String.format(
                    "Structural Error: Node ['%s'] refers to non-existent node ['%s'] as nextActivityId.",
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
                            "Structural Error: Branch condition in Node ['%s'] refers to non-existent node ['%s'] as targetActivityId.",
                            activity.id(), targetId
                    ));
                }
            }
        }
    }
}