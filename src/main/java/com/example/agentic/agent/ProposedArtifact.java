package com.example.agentic.agent;

import com.example.agentic.artifact.ArtifactType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What an agent proposes to persist as an Artifact. The orchestrator assigns the
 * id, version, and node linkage — the agent never writes to the artifact store
 * directly (ADR-6).
 */
public record ProposedArtifact(ArtifactType type, Map<String, Object> content, List<UUID> sourceArtifactIds) {

    public static ProposedArtifact of(ArtifactType type, Map<String, Object> content) {
        return new ProposedArtifact(type, content, List.of());
    }
}
