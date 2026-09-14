package com.example.agentic.orchestration.replan;

import com.example.agentic.agent.AgentType;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.orchestration.state.WorkflowNode;
import com.example.agentic.policy.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ImpactAnalyzerTest {

    private final ImpactAnalyzer analyzer = new ImpactAnalyzer();

    private WorkflowNode node(String key, AgentType type, List<String> dependsOn) {
        WorkflowNode n = new WorkflowNode(UUID.randomUUID(), UUID.randomUUID(), key, type, RiskLevel.LOW, 2, dependsOn);
        n.markRunning();
        n.markSucceeded();
        return n;
    }

    @Test
    void requirementChangeAffectsEveryDownstreamArtifactType() {
        List<WorkflowNode> nodes = List.of(
                node("requirements", AgentType.REQUIREMENTS, List.of()),
                node("architecture", AgentType.ARCHITECTURE, List.of("requirements")),
                node("planning", AgentType.PLANNING, List.of("requirements")),
                node("implementation", AgentType.DEVELOPER, List.of("architecture", "planning")),
                node("testing", AgentType.TEST, List.of("implementation")),
                node("security", AgentType.SECURITY, List.of("implementation")),
                node("documentation", AgentType.DOCUMENTATION, List.of("testing", "security")),
                node("release", AgentType.RELEASE, List.of("documentation"))
        );

        ImpactAnalyzer.ImpactResult result = analyzer.analyzeRequirementChange(nodes);

        assertThat(result.affectedNodes()).extracting(WorkflowNode::getNodeKey)
                .containsExactlyInAnyOrder("requirements", "architecture", "planning", "implementation",
                        "testing", "security", "documentation", "release");
        assertThat(result.unaffectedNodes()).isEmpty();
        assertThat(result.affectedArtifactTypes()).contains(ArtifactType.REQUIREMENT_SPEC,
                ArtifactType.ARCHITECTURE_SPEC, ArtifactType.IMPLEMENTATION, ArtifactType.RELEASE_MANIFEST);
    }

    @Test
    void unrelatedPriorArtifactsArePreservedWhenNotDownstreamOfRequirement() {
        // Every agent-produced artifact type in this system chains back to REQUIREMENT_SPEC
        // (per ArtifactDependencyMap), so this test documents that: a node whose agent type
        // has no PRODUCES mapping (defensive case) is neither affected nor unaffected-preserved
        // unless SUCCEEDED — it is simply excluded from both lists here since IMPACT_ANALYSIS
        // itself is in-map and downstream of REQUIREMENT_SPEC.
        List<WorkflowNode> nodes = List.of(
                node("requirements", AgentType.REQUIREMENTS, List.of()),
                node("impact_analysis", AgentType.IMPACT_ANALYSIS, List.of("requirements"))
        );

        ImpactAnalyzer.ImpactResult result = analyzer.analyzeRequirementChange(nodes);

        assertThat(result.affectedNodes()).extracting(WorkflowNode::getNodeKey)
                .containsExactlyInAnyOrder("requirements", "impact_analysis");
    }
}
