package com.example.agentic.orchestration.replan;

import com.example.agentic.agent.AgentType;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.orchestration.state.NodeStatus;
import com.example.agentic.orchestration.state.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Walks ArtifactDependencyMap from REQUIREMENT_SPEC to find every downstream
 * artifact type affected by a requirement change, then maps those artifact
 * types to the concrete WorkflowNode rows that produced them — this is what
 * makes re-planning selective rather than "start over" (01-architecture.md §8).
 */
@Component
public class ImpactAnalyzer {

    private static final Map<AgentType, ArtifactType> PRODUCES = Map.ofEntries(
            Map.entry(AgentType.REQUIREMENTS, ArtifactType.REQUIREMENT_SPEC),
            Map.entry(AgentType.IMPACT_ANALYSIS, ArtifactType.IMPACT_ANALYSIS),
            Map.entry(AgentType.ARCHITECTURE, ArtifactType.ARCHITECTURE_SPEC),
            Map.entry(AgentType.PLANNING, ArtifactType.IMPLEMENTATION_PLAN),
            Map.entry(AgentType.DEVELOPER, ArtifactType.IMPLEMENTATION),
            Map.entry(AgentType.TEST, ArtifactType.TEST_REPORT),
            Map.entry(AgentType.SECURITY, ArtifactType.SECURITY_REPORT),
            Map.entry(AgentType.DOCUMENTATION, ArtifactType.DOCUMENTATION),
            Map.entry(AgentType.RELEASE, ArtifactType.RELEASE_MANIFEST)
    );

    public record ImpactResult(Set<ArtifactType> affectedArtifactTypes, List<WorkflowNode> affectedNodes,
                                List<WorkflowNode> unaffectedNodes) {
    }

    public ImpactResult analyzeRequirementChange(List<WorkflowNode> allNodes) {
        Set<ArtifactType> affectedTypes = ArtifactDependencyMap.transitiveDownstream(ArtifactType.REQUIREMENT_SPEC);
        affectedTypes.add(ArtifactType.REQUIREMENT_SPEC);

        List<WorkflowNode> affected = allNodes.stream()
                .filter(n -> affectedTypes.contains(PRODUCES.get(n.getAgentType())))
                .toList();
        List<WorkflowNode> unaffected = allNodes.stream()
                .filter(n -> !affectedTypes.contains(PRODUCES.get(n.getAgentType())))
                .filter(n -> n.getStatus() == NodeStatus.SUCCEEDED)
                .toList();

        return new ImpactResult(affectedTypes, affected, unaffected);
    }

    public ArtifactType artifactTypeFor(AgentType agentType) {
        return PRODUCES.get(agentType);
    }
}
