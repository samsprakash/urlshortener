package com.example.agentic.agent;

import com.example.agentic.agent.llm.LlmClient;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.policy.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detects ambiguity rather than hallucinating assumptions — the core
 * differentiator for Scenario 3 (03-scenarios.md). Returns a structured
 * ambiguities[] list; the orchestrator, not this agent, decides to park the
 * workflow at CLARIFICATION_REQUIRED.
 */
@Component
public class RequirementsAgent implements Agent {

    private static final List<String> VAGUE_TERMS = List.of(
            "highly scalable", "scalable", "fast", "faster", "improve", "better", "optimize", "performance"
    );

    private final LlmClient llmClient;

    public RequirementsAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentType type() {
        return AgentType.REQUIREMENTS;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete(
                "You are a requirements analyst for a URL shortener system. Identify ambiguities that "
                        + "block safe design work; do not invent assumptions.",
                "Requirement: " + context.requirementText()
        );

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.LOW,
                "Analyze requirement for ambiguity and produce a RequirementSpec");

        List<String> ambiguities = detectAmbiguities(context.requirementText());
        if (!ambiguities.isEmpty()) {
            return AgentResult.clarificationRequired(action, ambiguities);
        }

        Map<String, Object> spec = Map.of(
                "requirementText", context.requirementText(),
                "status", "CLEAR",
                "summary", summarize(context.requirementText())
        );
        return AgentResult.success(action, List.of(ProposedArtifact.of(ArtifactType.REQUIREMENT_SPEC, spec)), List.of());
    }

    private List<String> detectAmbiguities(String requirementText) {
        String lower = requirementText.toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        boolean vague = VAGUE_TERMS.stream().anyMatch(lower::contains);
        if (!vague) {
            return found;
        }
        if (lower.contains("scalab")) {
            found.add("What traffic volume defines 'highly scalable'?");
            found.add("What availability target (99.9%? 99.99%)?");
        }
        if (lower.contains("fast") || lower.contains("performance") || lower.contains("latency")) {
            found.add("What latency target for redirects?");
        }
        if (lower.contains("analytics")) {
            found.add("Which analytics queries need to be faster, and how much faster?");
            found.add("What consistency requirements for analytics (eventual OK?)");
        }
        if (lower.contains("improve") || lower.contains("optimize") || lower.contains("better")) {
            found.add("What data retention period applies?");
        }
        return found;
    }

    private String summarize(String requirementText) {
        return requirementText.length() > 140 ? requirementText.substring(0, 140) + "..." : requirementText;
    }
}
