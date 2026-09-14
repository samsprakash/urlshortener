package com.example.agentic.agent;

import com.example.agentic.agent.llm.LlmClient;
import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.policy.RiskLevel;
import com.example.agentic.tool.EngineeringTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the (simulated) test suite. Honors metadata["forceTestFailure"] so the
 * demo script (03-scenarios.md step 5) can deterministically trigger
 * retry -> retry -> fallback/rollback on this node.
 */
@Component
public class TestAgent implements Agent {

    private final LlmClient llmClient;
    private final List<EngineeringTool> tools;

    public TestAgent(LlmClient llmClient, List<EngineeringTool> tools) {
        this.llmClient = llmClient;
        this.tools = tools;
    }

    @Override
    public AgentType type() {
        return AgentType.TEST;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        llmClient.complete("You are a test engineer.", "Requirement: " + context.requirementText());

        AgentAction action = new AgentAction(type(), context.nodeKey(), RiskLevel.MEDIUM,
                "Run test suite against implementation");

        boolean forceFailure = Boolean.TRUE.equals(context.metadataValue("forceTestFailure"));
        Map<String, Object> testRun = findTool("run_test").execute(Map.of("forceFailure", forceFailure));

        Artifact implementation = context.artifact(ArtifactType.IMPLEMENTATION);
        List<UUID> sources = implementation == null ? List.of() : List.of(implementation.getId());

        boolean passed = Boolean.TRUE.equals(testRun.get("passed"));
        if (!passed) {
            return AgentResult.failure(action, (String) testRun.getOrDefault("error", "Tests failed"));
        }

        Map<String, Object> report = Map.of(
                "passed", true,
                "testsRun", testRun.get("testsRun"),
                "testsFailed", testRun.get("testsFailed")
        );
        return AgentResult.success(action,
                List.of(new ProposedArtifact(ArtifactType.TEST_REPORT, report, sources)), List.of());
    }

    private EngineeringTool findTool(String name) {
        return tools.stream().filter(t -> t.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No tool registered: " + name));
    }
}
