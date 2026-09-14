package com.example.agentic.orchestration.api;

import com.example.agentic.orchestration.state.Workflow;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String scenario,
        String status,
        String requirementText,
        int requirementVersion,
        String correlationId,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        List<NodeResponse> nodes
) {
    public record NodeResponse(UUID id, String nodeKey, String agentType, String status, String riskLevel,
                                int retryCount, int maxRetries, List<String> dependsOn, String errorMessage) {
    }

    public static WorkflowResponse from(Workflow workflow, List<com.example.agentic.orchestration.state.WorkflowNode> nodes) {
        List<NodeResponse> nodeResponses = nodes.stream()
                .map(n -> new NodeResponse(n.getId(), n.getNodeKey(), n.getAgentType().name(), n.getStatus().name(),
                        n.getRiskLevel().name(), n.getRetryCount(), n.getMaxRetries(), n.getDependsOn(), n.getErrorMessage()))
                .toList();
        return new WorkflowResponse(workflow.getId(), workflow.getScenario().name(), workflow.getStatus().name(),
                workflow.getRequirementText(), workflow.getRequirementVersion(), workflow.getCorrelationId(),
                workflow.getCreatedAt(), workflow.getUpdatedAt(), workflow.getCompletedAt(), nodeResponses);
    }
}
