package com.example.agentic.agent;

import com.example.agentic.agent.llm.LlmClient;
import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.policy.RiskLevel;
import com.example.agentic.tool.EngineeringTool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DeveloperAgent implements Agent {

    private final LlmClient llmClient;
    private final List<EngineeringTool> tools;

    public DeveloperAgent(LlmClient llmClient, List<EngineeringTool> tools) {
        this.llmClient = llmClient;
        this.tools = tools;
    }

    @Override
    public AgentType type() {
        return AgentType.DEVELOPER;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete("You are a senior Java developer implementing a URL shortener feature.",
                "Requirement: " + context.requirementText());

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.MEDIUM,
                "Generate implementation against ArchitectureSpec/ImplementationPlan");

        Map<String, Object> diff = findTool("git_diff").execute(Map.of("nodeKey", context.nodeKey()));

        List<UUID> sources = new ArrayList<>();
        Artifact arch = context.artifact(ArtifactType.ARCHITECTURE_SPEC);
        Artifact plan = context.artifact(ArtifactType.IMPLEMENTATION_PLAN);
        if (arch != null) sources.add(arch.getId());
        if (plan != null) sources.add(plan.getId());

        Map<String, Object> implementation = Map.of(
                "filesChanged", diff.get("filesChanged"),
                "insertions", diff.get("insertions"),
                "deletions", diff.get("deletions"),
                "summary", "Implementation generated for node " + context.nodeKey()
        );
        return AgentResult.success(action,
                List.of(new ProposedArtifact(ArtifactType.IMPLEMENTATION, implementation, sources)), List.of());
    }

    private EngineeringTool findTool(String name) {
        return tools.stream().filter(t -> t.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No tool registered: " + name));
    }
}
