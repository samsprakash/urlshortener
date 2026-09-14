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

@Component
public class DocumentationAgent implements Agent {

    private final LlmClient llmClient;

    public DocumentationAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public AgentType type() {
        return AgentType.DOCUMENTATION;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete("You are a technical writer.", "Requirement: " + context.requirementText());

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.LOW,
                "Generate documentation summarizing the change");

        List<UUID> sources = new ArrayList<>();
        Artifact testReport = context.artifact(ArtifactType.TEST_REPORT);
        Artifact securityReport = context.artifact(ArtifactType.SECURITY_REPORT);
        if (testReport != null) sources.add(testReport.getId());
        if (securityReport != null) sources.add(securityReport.getId());

        Map<String, Object> docs = Map.of(
                "summary", "Documentation generated for requirement: " + context.requirementText(),
                "sections", List.of("Overview", "API changes", "Testing", "Security")
        );
        return AgentResult.success(action,
                List.of(new ProposedArtifact(ArtifactType.DOCUMENTATION, docs, sources)), List.of());
    }
}
