package com.example.agentic.orchestration.graph;

import com.example.agentic.agent.AgentType;
import com.example.agentic.policy.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowGraphTest {

    @Test
    void buildsValidAcyclicGraph() {
        WorkflowGraph graph = WorkflowGraph.of(List.of(
                GraphNodeSpec.of("a", AgentType.REQUIREMENTS, RiskLevel.LOW),
                GraphNodeSpec.of("b", AgentType.ARCHITECTURE, RiskLevel.LOW, "a")
        ));
        assertThat(graph.nodes()).hasSize(2);
        assertThat(graph.get("b").dependsOn()).containsExactly("a");
    }

    @Test
    void rejectsDuplicateNodeKeys() {
        assertThatThrownBy(() -> WorkflowGraph.of(List.of(
                GraphNodeSpec.of("a", AgentType.REQUIREMENTS, RiskLevel.LOW),
                GraphNodeSpec.of("a", AgentType.ARCHITECTURE, RiskLevel.LOW)
        ))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");
    }

    @Test
    void rejectsDanglingDependency() {
        assertThatThrownBy(() -> WorkflowGraph.of(List.of(
                GraphNodeSpec.of("a", AgentType.REQUIREMENTS, RiskLevel.LOW, "nonexistent")
        ))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown node");
    }

    @Test
    void rejectsCycle() {
        assertThatThrownBy(() -> WorkflowGraph.of(List.of(
                GraphNodeSpec.of("a", AgentType.REQUIREMENTS, RiskLevel.LOW, "b"),
                GraphNodeSpec.of("b", AgentType.ARCHITECTURE, RiskLevel.LOW, "a")
        ))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cycle");
    }

    @Test
    void greenfieldGraphHasExpectedShape() {
        WorkflowGraph graph = WorkflowGraphs.greenfield();
        assertThat(graph.get("architecture").dependsOn()).containsExactly("requirements");
        assertThat(graph.get("planning").dependsOn()).containsExactly("requirements");
        assertThat(graph.get("implementation").dependsOn()).containsExactlyInAnyOrder("architecture", "planning");
        assertThat(graph.get("testing").dependsOn()).containsExactly("implementation");
        assertThat(graph.get("security").dependsOn()).containsExactly("implementation");
        assertThat(graph.get("documentation").dependsOn()).containsExactlyInAnyOrder("testing", "security");
        assertThat(graph.get("release").dependsOn()).containsExactly("documentation");
    }

    @Test
    void brownfieldGraphMarksSchemaAsHighRisk() {
        WorkflowGraph graph = WorkflowGraphs.brownfield();
        assertThat(graph.get("schema").riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(graph.get("schema").dependsOn()).containsExactly("impact_analysis");
    }
}
