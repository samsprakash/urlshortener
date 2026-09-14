package com.example.agentic.orchestration.engine;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo/test control knobs (e.g. forceTestFailure, forceSecurityFailure) keyed
 * by workflow id. Deliberately not persisted: this is a lever for triggering
 * deterministic failure-path demonstrations (03-scenarios.md step 5), not
 * domain state, so it does not belong in the audited workflow_nodes table.
 */
@Component
public class WorkflowRuntimeMetadata {

    private final Map<UUID, Map<String, Object>> metadataByWorkflow = new ConcurrentHashMap<>();

    public void put(UUID workflowId, Map<String, Object> metadata) {
        metadataByWorkflow.put(workflowId, Map.copyOf(metadata));
    }

    public Map<String, Object> get(UUID workflowId) {
        return metadataByWorkflow.getOrDefault(workflowId, Map.of());
    }
}
