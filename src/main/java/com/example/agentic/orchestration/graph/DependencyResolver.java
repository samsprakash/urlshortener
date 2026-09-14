package com.example.agentic.orchestration.graph;

import com.example.agentic.orchestration.state.NodeStatus;
import com.example.agentic.orchestration.state.WorkflowNode;

import java.util.*;

/**
 * Pure functions over node dependency edges: cycle detection at graph-build time,
 * and "is this node ready to run" at schedule time. Kept separate from
 * WorkflowEngine so the dependency logic is unit-testable without a database.
 */
public final class DependencyResolver {

    private DependencyResolver() {
    }

    public static void assertAcyclic(Map<String, GraphNodeSpec> byKey) {
        Set<String> visited = new HashSet<>();
        Set<String> inProgress = new HashSet<>();
        for (String key : byKey.keySet()) {
            if (!visited.contains(key)) {
                visit(key, byKey, visited, inProgress);
            }
        }
    }

    private static void visit(String key, Map<String, GraphNodeSpec> byKey, Set<String> visited, Set<String> inProgress) {
        inProgress.add(key);
        for (String dep : byKey.get(key).dependsOn()) {
            if (inProgress.contains(dep)) {
                throw new IllegalArgumentException("Cycle detected in workflow graph involving '" + dep + "'");
            }
            if (!visited.contains(dep)) {
                visit(dep, byKey, visited, inProgress);
            }
        }
        inProgress.remove(key);
        visited.add(key);
    }

    /**
     * A node is ready when every dependency has SUCCEEDED (INVALIDATED/pre-existing
     * dependencies from before a re-plan are treated as satisfied only if SUCCEEDED —
     * an invalidated upstream artifact must be re-run before downstream can proceed).
     */
    public static boolean isReady(WorkflowNode node, Map<String, WorkflowNode> allNodesByKey) {
        if (node.getStatus() != NodeStatus.PENDING) {
            return false;
        }
        for (String depKey : node.getDependsOn()) {
            WorkflowNode dep = allNodesByKey.get(depKey);
            if (dep == null || dep.getStatus() != NodeStatus.SUCCEEDED) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBlockedByFailedDependency(WorkflowNode node, Map<String, WorkflowNode> allNodesByKey) {
        if (node.getStatus() != NodeStatus.PENDING) {
            return false;
        }
        for (String depKey : node.getDependsOn()) {
            WorkflowNode dep = allNodesByKey.get(depKey);
            if (dep != null && (dep.getStatus() == NodeStatus.FAILED
                    || dep.getStatus() == NodeStatus.BLOCKED
                    || dep.getStatus() == NodeStatus.ROLLED_BACK)) {
                return true;
            }
        }
        return false;
    }
}
