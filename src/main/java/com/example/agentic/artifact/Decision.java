package com.example.agentic.artifact;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "decisions")
public class Decision {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "artifact_id")
    private UUID artifactId;

    @Column(name = "decision", nullable = false, length = 256)
    private String decision;

    @Column(name = "rationale", nullable = false)
    private String rationale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alternatives", nullable = false, columnDefinition = "jsonb")
    private List<String> alternatives;

    @Column(name = "selected", nullable = false, length = 256)
    private String selected;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Decision() {
        // JPA
    }

    public Decision(UUID id, UUID workflowId, UUID artifactId, String decision, String rationale,
                     List<String> alternatives, String selected) {
        this.id = id;
        this.workflowId = workflowId;
        this.artifactId = artifactId;
        this.decision = decision;
        this.rationale = rationale;
        this.alternatives = alternatives;
        this.selected = selected;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public UUID getArtifactId() {
        return artifactId;
    }

    public String getDecision() {
        return decision;
    }

    public String getRationale() {
        return rationale;
    }

    public List<String> getAlternatives() {
        return alternatives;
    }

    public String getSelected() {
        return selected;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
