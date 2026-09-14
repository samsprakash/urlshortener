package com.example.agentic.approval;

import com.example.agentic.policy.RiskLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Column(name = "action", nullable = false, length = 256)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ApprovalStatus status;

    @Column(name = "approver", length = 128)
    private String approver;

    @Column(name = "comment")
    private String comment;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected Approval() {
        // JPA
    }

    public Approval(UUID id, UUID workflowId, UUID nodeId, String action, RiskLevel riskLevel) {
        this.id = id;
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.action = action;
        this.riskLevel = riskLevel;
        this.status = ApprovalStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public void approve(String approver, String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.approver = approver;
        this.comment = comment;
        this.decidedAt = Instant.now();
    }

    public void reject(String approver, String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.approver = approver;
        this.comment = comment;
        this.decidedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getAction() {
        return action;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public String getApprover() {
        return approver;
    }

    public String getComment() {
        return comment;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
