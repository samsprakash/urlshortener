package com.example.agentic.artifact;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "artifacts")
public class Artifact {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_id")
    private UUID nodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private ArtifactType type;

    @Column(name = "version", nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_artifact_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> sourceArtifactIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ArtifactStatus status;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Artifact() {
        // JPA
    }

    public Artifact(UUID id, UUID workflowId, UUID nodeId, ArtifactType type, int version,
                     Map<String, Object> content, List<UUID> sourceArtifactIds, String createdBy) {
        this.id = id;
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.type = type;
        this.version = version;
        this.content = content;
        this.sourceArtifactIds = sourceArtifactIds;
        this.status = ArtifactStatus.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public void invalidate() {
        this.status = ArtifactStatus.INVALIDATED;
    }

    public void supersede() {
        this.status = ArtifactStatus.SUPERSEDED;
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

    public ArtifactType getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public List<UUID> getSourceArtifactIds() {
        return sourceArtifactIds;
    }

    public ArtifactStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
