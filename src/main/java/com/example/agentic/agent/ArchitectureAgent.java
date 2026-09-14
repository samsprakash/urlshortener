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
 * Produces the ArchitectureSpec artifact and records the Base62-short-code
 * decision with rationale + alternatives — the concrete example from
 * 03-scenarios.md's Greenfield walkthrough. For the "schema" node key
 * (Brownfield), instead proposes a schema-change spec, which ChangeControlPolicy
 * always routes to human approval regardless of declared risk.
 */
@Component
public class ArchitectureAgent implements Agent {

    private final LlmClient llmClient;

    public ArchitectureAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentType type() {
        return AgentType.ARCHITECTURE;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete(
                "You are a software architect designing a URL shortener.",
                "Requirement: " + context.requirementText()
        );

        boolean isSchemaChange = "schema".equals(context.nodeKey());
        RiskLevel risk = isSchemaChange ? RiskLevel.HIGH : RiskLevel.LOW;
        AgentAction action = new AgentAction(type(), context.nodeKey(), risk,
                isSchemaChange ? "Propose schema change for configurable URL expiration"
                        : "Design short-code strategy and system architecture");

        Artifact requirementSpec = context.artifact(ArtifactType.REQUIREMENT_SPEC);
        List<UUID> sources = requirementSpec == null ? List.of() : List.of(requirementSpec.getId());

        if (isSchemaChange) {
            Map<String, Object> schemaSpec = Map.of(
                    "change", "Add nullable expires_at override support and status transition on expiry",
                    "migration", "ALTER TABLE urls ALTER COLUMN expires_at DROP NOT NULL (already nullable); add index on expires_at",
                    "backwardCompatible", true,
                    "riskLevel", "HIGH"
            );
            return AgentResult.success(action, List.of(new com.example.agentic.agent.ProposedArtifact(
                    ArtifactType.ARCHITECTURE_SPEC, schemaSpec, sources)), List.of());
        }

        Map<String, Object> archSpec = Map.of(
                "shortCodeStrategy", "Base62",
                "shortCodeLength", 7,
                "storage", "PostgreSQL",
                "components", List.of("UrlController", "UrlService", "RedirectService", "ClickTrackingService")
        );
        ProposedDecision decision = new ProposedDecision(
                "Use Base62 for short codes",
                "Compact representation, sufficient keyspace, URL-safe alphabet",
                List.of("UUID", "Hashids"),
                "Base62"
        );
        return AgentResult.success(action,
                List.of(new com.example.agentic.agent.ProposedArtifact(ArtifactType.ARCHITECTURE_SPEC, archSpec, sources)),
                List.of(decision));
    }
}
