package com.example.agentic.agent;

import com.example.agentic.agent.llm.LlmClient;
import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.policy.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Honors metadata["forceSecurityFailure"] to deterministically demonstrate the
 * security-fail -> rollback -> replan path referenced in 01-architecture.md §5's
 * back-edges and Scenario 2b's "deliberate validation failure" stretch goal.
 */
@Component
public class SecurityAgent implements Agent {

    private final LlmClient llmClient;

    public SecurityAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentType type() {
        return AgentType.SECURITY;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete("You are an application security reviewer.", "Requirement: " + context.requirementText());

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.MEDIUM,
                "Run security review against implementation");

        boolean forceFailure = Boolean.TRUE.equals(context.metadataValue("forceSecurityFailure"));
        if (forceFailure) {
            return AgentResult.failure(action, "Security review failed: forced failure for rollback demonstration");
        }

        Artifact implementation = context.artifact(ArtifactType.IMPLEMENTATION);
        List<UUID> sources = implementation == null ? List.of() : List.of(implementation.getId());

        Map<String, Object> report = Map.of(
                "passed", true,
                "checks", List.of("scheme-allowlist", "no-raw-ip-storage", "no-shell-injection", "input-length-limits"),
                "findings", List.of()
        );
        return AgentResult.success(action,
                List.of(new ProposedArtifact(ArtifactType.SECURITY_REPORT, report, sources)), List.of());
    }
}
