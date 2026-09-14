package com.example.agentic.orchestration.replan;

import com.example.agentic.artifact.ArtifactType;

import java.util.Map;
import java.util.Set;

/**
 * Explicit ArtifactType -> Set<ArtifactType> dependency map, per
 * 01-architecture.md §8. When a requirement changes, ImpactAnalyzer walks this
 * map forward from REQUIREMENT_SPEC to find every artifact type that is
 * downstream of (and therefore invalidated by) the change.
 */
public final class ArtifactDependencyMap {

    private static final Map<ArtifactType, Set<ArtifactType>> DOWNSTREAM = Map.of(
            ArtifactType.REQUIREMENT_SPEC, Set.of(ArtifactType.IMPACT_ANALYSIS, ArtifactType.ARCHITECTURE_SPEC, ArtifactType.IMPLEMENTATION_PLAN),
            ArtifactType.IMPACT_ANALYSIS, Set.of(ArtifactType.ARCHITECTURE_SPEC, ArtifactType.IMPLEMENTATION_PLAN, ArtifactType.IMPLEMENTATION),
            ArtifactType.ARCHITECTURE_SPEC, Set.of(ArtifactType.IMPLEMENTATION),
            ArtifactType.IMPLEMENTATION_PLAN, Set.of(ArtifactType.IMPLEMENTATION),
            ArtifactType.IMPLEMENTATION, Set.of(ArtifactType.TEST_REPORT, ArtifactType.SECURITY_REPORT),
            ArtifactType.TEST_REPORT, Set.of(ArtifactType.DOCUMENTATION, ArtifactType.RELEASE_MANIFEST),
            ArtifactType.SECURITY_REPORT, Set.of(ArtifactType.DOCUMENTATION, ArtifactType.RELEASE_MANIFEST),
            ArtifactType.DOCUMENTATION, Set.of(ArtifactType.RELEASE_MANIFEST)
    );

    private ArtifactDependencyMap() {
    }

    public static Set<ArtifactType> directDownstream(ArtifactType type) {
        return DOWNSTREAM.getOrDefault(type, Set.of());
    }

    public static Set<ArtifactType> transitiveDownstream(ArtifactType root) {
        Set<ArtifactType> result = new java.util.HashSet<>();
        java.util.Deque<ArtifactType> queue = new java.util.ArrayDeque<>(directDownstream(root));
        while (!queue.isEmpty()) {
            ArtifactType next = queue.poll();
            if (result.add(next)) {
                queue.addAll(directDownstream(next));
            }
        }
        return result;
    }
}
