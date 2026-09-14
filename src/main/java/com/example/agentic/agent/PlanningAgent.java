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
 * Task decomposition: turns the requirement into an ordered implementation
 * plan (01-architecture.md §11). For the Brownfield "test_plan" node key,
 * produces a regression-focused plan instead of a net-new one, per
 * 03-scenarios.md Scenario 2.
 */
@Component
public class PlanningAgent implements Agent {

    private final LlmClient llmClient;

    public PlanningAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentType type() {
        return AgentType.PLANNING;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete("You are a technical planner.", "Requirement: " + context.requirementText());

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.LOW,
                "Decompose requirement into an ordered implementation plan");

        Artifact requirementSpec = context.artifact(ArtifactType.REQUIREMENT_SPEC);
        List<UUID> sources = requirementSpec == null ? List.of() : List.of(requirementSpec.getId());

        boolean isRegressionPlan = "test_plan".equals(context.nodeKey());
        Map<String, Object> plan = isRegressionPlan
                ? Map.of(
                        "planType", "REGRESSION",
                        "steps", List.of(
                                "Re-run existing create/redirect/analytics test suites unchanged",
                                "Add expiry-specific test cases (default, override, already-expired)",
                                "Verify existing clients unaffected by contract diff"))
                : Map.of(
                        "planType", "NET_NEW",
                        "steps", List.of(
                                "Implement UrlService.createShortUrl",
                                "Implement RedirectService.resolve",
                                "Implement ClickTrackingService.recordClick",
                                "Wire REST controllers"));

        return AgentResult.success(action,
                List.of(new ProposedArtifact(ArtifactType.IMPLEMENTATION_PLAN, plan, sources)), List.of());
    }
}
