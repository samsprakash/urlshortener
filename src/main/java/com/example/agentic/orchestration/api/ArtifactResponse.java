package com.example.agentic.orchestration.api;

import com.example.agentic.artifact.Artifact;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ArtifactResponse(UUID id, String type, int version, Map<String, Object> content,
                                List<UUID> sourceArtifactIds, String status, String createdBy, Instant createdAt) {
    public static ArtifactResponse from(Artifact artifact) {
        return new ArtifactResponse(artifact.getId(), artifact.getType().name(), artifact.getVersion(),
                artifact.getContent(), artifact.getSourceArtifactIds(), artifact.getStatus().name(),
                artifact.getCreatedBy(), artifact.getCreatedAt());
    }
}
