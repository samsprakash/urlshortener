package com.example.agentic.agent;

import com.example.agentic.agent.llm.LlmClient;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.policy.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reasons about the EXISTING system before any code changes — the difference
 * between "generate new code" and "safely change a system you didn't just
 * build" (03-scenarios.md, Scenario 2 / Brownfield).
 */
@Component
public class ImpactAnalysisAgent implements Agent {

    private final LlmClient llmClient;

    public ImpactAnalysisAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentType type() {
        return AgentType.IMPACT_ANALYSIS;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete(
                "You are a staff engineer performing impact analysis on an existing URL-shortener codebase.",
                "Requirement: " + context.requirementText()
        );

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.LOW,
                "Analyze existing codebase for components affected by the requirement");

        boolean schemaChange = requiresSchemaChange(context.requirementText());
        boolean touchesEventing = touchesEventing(context.requirementText());

        List<String> affected = new java.util.ArrayList<>(List.of("Url entity", "urls table", "CreateUrl API",
                "RedirectService", "unit tests", "docs"));
        List<String> unaffected = new java.util.ArrayList<>(List.of("UrlController create/get endpoints", "analytics read API"));
        if (touchesEventing) {
            affected.add("EventPublisher");
            affected.add("ClickTrackingService");
        }

        Map<String, Object> analysis = Map.of(
                "requiresSchemaChange", schemaChange,
                "touchesEventing", touchesEventing,
                "affectedComponents", affected,
                "unaffectedComponents", unaffected
        );
        return AgentResult.success(action, List.of(ProposedArtifact.of(ArtifactType.IMPACT_ANALYSIS, analysis)), List.of());
    }

    private boolean requiresSchemaChange(String requirementText) {
        String lower = requirementText.toLowerCase(Locale.ROOT);
        return lower.contains("expir") || lower.contains("schema") || lower.contains("column") || lower.contains("field");
    }

    private boolean touchesEventing(String requirementText) {
        String lower = requirementText.toLowerCase(Locale.ROOT);
        return lower.contains("kafka") || lower.contains("async") || lower.contains("event");
    }
}
