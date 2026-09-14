package com.example.agentic.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(name = "node_id")
    private UUID nodeId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor", nullable = false, length = 64)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private AuditOutcome outcome;

    @Column(name = "reason")
    private String reason;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "artifact_versions", nullable = false, columnDefinition = "jsonb")
    private Map<String, Integer> artifactVersions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
        // JPA
    }

    public AuditEvent(UUID id, UUID workflowId, UUID nodeId, String eventType, String actor,
                       AuditOutcome outcome, String reason, String correlationId,
                       Map<String, Integer> artifactVersions) {
        this.id = id;
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.eventType = eventType;
        this.actor = actor;
        this.outcome = outcome;
        this.reason = reason;
        this.correlationId = correlationId;
        this.artifactVersions = artifactVersions;
        this.createdAt = Instant.now();
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

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Map<String, Integer> getArtifactVersions() {
        return artifactVersions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
