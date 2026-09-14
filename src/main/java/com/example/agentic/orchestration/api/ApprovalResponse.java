package com.example.agentic.orchestration.api;

import com.example.agentic.approval.Approval;

import java.time.Instant;
import java.util.UUID;

public record ApprovalResponse(UUID id, UUID workflowId, UUID nodeId, String action, String riskLevel,
                                String status, String approver, String comment, Instant requestedAt, Instant decidedAt) {
    public static ApprovalResponse from(Approval approval) {
        return new ApprovalResponse(approval.getId(), approval.getWorkflowId(), approval.getNodeId(),
                approval.getAction(), approval.getRiskLevel().name(), approval.getStatus().name(),
                approval.getApprover(), approval.getComment(), approval.getRequestedAt(), approval.getDecidedAt());
    }
}
