package com.example.agentic.orchestration.graph;

import com.example.agentic.agent.AgentType;
import com.example.agentic.policy.RiskLevel;

import java.util.List;
import java.util.Set;

/**
 * Blueprint for a single node before it is materialized as a WorkflowNode row.
 * A WorkflowGraph is a set of these plus their dependency edges — this is what
 * lets the same shape (with a different subset of nodes) drive Greenfield,
 * Brownfield, and Ambiguous, per 03-scenarios.md.
 */
public record GraphNodeSpec(String nodeKey, AgentType agentType, RiskLevel riskLevel,
                             int maxRetries, Set<String> dependsOn) {

    public static GraphNodeSpec of(String nodeKey, AgentType agentType, RiskLevel riskLevel, String... dependsOn) {
        return new GraphNodeSpec(nodeKey, agentType, riskLevel, 2, Set.of(dependsOn));
    }

    public static GraphNodeSpec of(String nodeKey, AgentType agentType, RiskLevel riskLevel, int maxRetries, String... dependsOn) {
        return new GraphNodeSpec(nodeKey, agentType, riskLevel, maxRetries, Set.of(dependsOn));
    }

    public List<String> dependsOnList() {
        return dependsOn.stream().sorted().toList();
    }
}
