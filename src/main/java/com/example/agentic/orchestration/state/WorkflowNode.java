package com.example.agentic.orchestration.state;

import com.example.agentic.agent.AgentType;
import com.example.agentic.policy.RiskLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workflow_nodes")
public class WorkflowNode {

    /** Transitions considered valid by the engine; anything else is a programming error. */
    private static final Set<NodeStatus> TERMINAL = Set.of(
            NodeStatus.SUCCEEDED, NodeStatus.ROLLED_BACK, NodeStatus.INVALIDATED
    );

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 64)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 64)
    private AgentType agentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NodeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "depends_on", nullable = false, columnDefinition = "jsonb")
    private List<String> dependsOn;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WorkflowNode() {
        // JPA
    }

    public WorkflowNode(UUID id, UUID workflowId, String nodeKey, AgentType agentType,
                         RiskLevel riskLevel, int maxRetries, List<String> dependsOn) {
        this.id = id;
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.agentType = agentType;
        this.status = NodeStatus.PENDING;
        this.riskLevel = riskLevel;
        this.retryCount = 0;
        this.maxRetries = maxRetries;
        this.dependsOn = dependsOn;
        this.createdAt = Instant.now();
    }

    public boolean isTerminal() {
        return TERMINAL.contains(status);
    }

    public void markReady() {
        this.status = NodeStatus.READY;
    }

    public void markRunning() {
        this.status = NodeStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markSucceeded() {
        this.status = NodeStatus.SUCCEEDED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = NodeStatus.FAILED;
        this.errorMessage = message;
        this.completedAt = Instant.now();
    }

    public void markRetrying() {
        this.status = NodeStatus.RETRYING;
        this.retryCount++;
    }

    public void markWaitingForApproval() {
        this.status = NodeStatus.WAITING_FOR_APPROVAL;
    }

    public void markBlocked(String message) {
        this.status = NodeStatus.BLOCKED;
        this.errorMessage = message;
    }

    public void markRolledBack() {
        this.status = NodeStatus.ROLLED_BACK;
        this.completedAt = Instant.now();
    }

    public void markSafeStopped() {
        this.status = NodeStatus.SAFE_STOPPED;
    }

    public void markInvalidated() {
        this.status = NodeStatus.INVALIDATED;
    }

    public void resetForReplan() {
        this.status = NodeStatus.PENDING;
        this.retryCount = 0;
        this.errorMessage = null;
        this.startedAt = null;
        this.completedAt = null;
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
