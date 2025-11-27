package com.example.aicopilot.dto.process;

import com.example.aicopilot.dto.process.config.NodeConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Map;

/**
 * Model for an individual node (Activity) in the workflow.
 * Supports Polymorphism, where configuration varies based on node type.
 */
public record Activity(
        @JsonProperty("id")
        @JsonPropertyDescription("Unique Identifier. MUST be 'snake_case' (e.g., 'node_submit_req').")
        String id,

        @JsonProperty("type")
        @JsonPropertyDescription("Semantic Type of the node: USER_TASK, SERVICE_TASK, EXCLUSIVE_GATEWAY.")
        NodeType type,

        @JsonProperty("label")
        @JsonPropertyDescription("Human-readable label for UI display.")
        String label,

        @JsonProperty("swimlaneId")
        @JsonPropertyDescription("The ID of the swimlane this node belongs to.")
        String swimlaneId,

        @JsonProperty("description")
        @JsonPropertyDescription("Brief description of the task performed by this node.")
        String description,

        @JsonProperty("configuration")
        @JsonPropertyDescription("Polymorphic configuration object based on node type (Unified Record).")
        NodeConfiguration configuration,

        @JsonProperty("inputMapping")
        @JsonPropertyDescription("Smart Data Binding (Key: Input field of this node, Value: #{SourceNode.Alias}).")
        Map<String, String> inputMapping,

        @JsonProperty("position")
        @JsonPropertyDescription("Optional UI coordinates {x, y} for visualization.")
        Map<String, Double> position,

        @JsonProperty("nextActivityId")
        @JsonPropertyDescription("""
                The Default Sequence Flow.
                - For TASKS: The single next step.
                - For GATEWAYS: The 'Else' path (if no conditions match).
                """)
        String nextActivityId
) {}