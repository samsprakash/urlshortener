package com.example.agentic.agent;

import com.example.agentic.agent.llm.LlmClient;
import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.policy.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Produces the ReleaseManifest. Declared risk is HIGH — ReleasePolicy and
 * ChangeControlPolicy independently escalate this to a required human approval
 * regardless, but the node's own declared risk documents that release is never
 * meant to be autonomous (01-architecture.md §4).
 */
@Component
public class ReleaseAgent implements Agent {

    private final LlmClient llmClient;

    public ReleaseAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentType type() {
        return AgentType.RELEASE;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete("You are a release manager.", "Requirement: " + context.requirementText());

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.HIGH,
                "Prepare release manifest for production deployment");

        List<UUID> sources = new ArrayList<>();
        Artifact docs = context.artifact(ArtifactType.DOCUMENTATION);
        Artifact testReport = context.artifact(ArtifactType.TEST_REPORT);
        Artifact securityReport = context.artifact(ArtifactType.SECURITY_REPORT);
        if (docs != null) sources.add(docs.getId());
        if (testReport != null) sources.add(testReport.getId());
        if (securityReport != null) sources.add(securityReport.getId());

        Map<String, Object> manifest = Map.of(
                "version", "1.0.0",
                "readyForRelease", true,
                "requirement", context.requirementText()
        );
        return AgentResult.success(action,
                List.of(new ProposedArtifact(ArtifactType.RELEASE_MANIFEST, manifest, sources)), List.of());
    }
}
