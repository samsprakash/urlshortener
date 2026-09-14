package com.example.agentic.audit.api;

import com.example.agentic.audit.AuditEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(UUID id, UUID workflowId, UUID nodeId, String eventType, String actor,
                                  String outcome, String reason, String correlationId,
                                  Map<String, Integer> artifactVersions, Instant createdAt) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getWorkflowId(), event.getNodeId(), event.getEventType(),
                event.getActor(), event.getOutcome().name(), event.getReason(), event.getCorrelationId(),
                event.getArtifactVersions(), event.getCreatedAt());
    }
}
