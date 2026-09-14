package com.example.agentic.orchestration.state;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflows")
public class Workflow {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", nullable = false, length = 32)
    private Scenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkflowStatus status;

    @Column(name = "requirement_text", nullable = false)
    private String requirementText;

    @Column(name = "requirement_version", nullable = false)
    private int requirementVersion;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Workflow() {
        // JPA
    }

    public Workflow(UUID id, Scenario scenario, String requirementText, String correlationId) {
        this.id = id;
        this.scenario = scenario;
        this.status = WorkflowStatus.RUNNING;
        this.requirementText = requirementText;
        this.requirementVersion = 1;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
        touch();
        if (status == WorkflowStatus.SUCCEEDED || status == WorkflowStatus.FAILED) {
            this.completedAt = Instant.now();
        }
    }

    public void bumpRequirementVersion(String newRequirementText) {
        this.requirementText = newRequirementText;
        this.requirementVersion++;
        touch();
    }

    public UUID getId() {
        return id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public int getRequirementVersion() {
        return requirementVersion;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
