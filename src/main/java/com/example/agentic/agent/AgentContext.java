package com.example.agentic.agent;

import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactType;

import java.util.Map;
import java.util.UUID;

/**
 * Everything an agent needs to reason, and nothing it needs to mutate state
 * directly with. Artifacts are the cross-stage context handoff (01-architecture.md §4);
 * agents read from this map, they never write to the database themselves.
 */
public record AgentContext(UUID workflowId, String nodeKey, String requirementText,
                            Map<ArtifactType, Artifact> artifacts, Map<String, Object> metadata) {

    public Artifact artifact(ArtifactType type) {
        return artifacts.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T metadataValue(String key) {
        return (T) metadata.get(key);
    }
}
