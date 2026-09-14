package com.example.agentic.orchestration.graph;

import com.example.agentic.agent.AgentType;
import com.example.agentic.orchestration.state.WorkflowNode;
import com.example.agentic.policy.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyResolverTest {

    private WorkflowNode newNode(String key, List<String> dependsOn) {
        return new WorkflowNode(UUID.randomUUID(), UUID.randomUUID(), key, AgentType.DEVELOPER,
                RiskLevel.LOW, 2, dependsOn);
    }

    @Test
    void nodeWithNoDependenciesIsReady() {
        WorkflowNode node = newNode("a", List.of());
        assertThat(DependencyResolver.isReady(node, Map.of("a", node))).isTrue();
    }

    @Test
    void nodeIsNotReadyUntilAllDependenciesSucceed() {
        WorkflowNode dep = newNode("a", List.of());
        WorkflowNode node = newNode("b", List.of("a"));
        Map<String, WorkflowNode> byKey = Map.of("a", dep, "b", node);

        assertThat(DependencyResolver.isReady(node, byKey)).isFalse();

        dep.markRunning();
        dep.markSucceeded();
        assertThat(DependencyResolver.isReady(node, byKey)).isTrue();
    }

    @Test
    void nodeRequiresAllDependenciesNotJustOne() {
        WorkflowNode depA = newNode("a", List.of());
        WorkflowNode depB = newNode("b", List.of());
        WorkflowNode node = newNode("c", List.of("a", "b"));
        Map<String, WorkflowNode> byKey = Map.of("a", depA, "b", depB, "c", node);

        depA.markRunning();
        depA.markSucceeded();
        assertThat(DependencyResolver.isReady(node, byKey)).isFalse();

        depB.markRunning();
        depB.markSucceeded();
        assertThat(DependencyResolver.isReady(node, byKey)).isTrue();
    }

    @Test
    void nodeIsBlockedWhenDependencyFailed() {
        WorkflowNode dep = newNode("a", List.of());
        WorkflowNode node = newNode("b", List.of("a"));
        dep.markRunning();
        dep.markFailed("boom");
        Map<String, WorkflowNode> byKey = Map.of("a", dep, "b", node);

        assertThat(DependencyResolver.isBlockedByFailedDependency(node, byKey)).isTrue();
        assertThat(DependencyResolver.isReady(node, byKey)).isFalse();
    }

    @Test
    void nonPendingNodeIsNeverReady() {
        WorkflowNode node = newNode("a", List.of());
        node.markRunning();
        assertThat(DependencyResolver.isReady(node, Map.of("a", node))).isFalse();
    }
}
