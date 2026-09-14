package com.example.agentic.agent;

import com.example.agentic.agent.llm.LlmClient;
import com.example.agentic.artifact.ArtifactType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementsAgentTest {

    private final LlmClient stubLlm = (system, user) -> "{}";
    private final RequirementsAgent agent = new RequirementsAgent(stubLlm);

    @Test
    void clearRequirementProducesRequirementSpecArtifact() {
        AgentContext context = new AgentContext(UUID.randomUUID(), "requirements",
                "Build a URL shortener that allows users to create short links and track clicks.",
                Map.of(), Map.of());

        AgentResult result = agent.execute(context);

        assertThat(result.success()).isTrue();
        assertThat(result.requiresClarification()).isFalse();
        assertThat(result.artifacts()).hasSize(1);
        assertThat(result.artifacts().get(0).type()).isEqualTo(ArtifactType.REQUIREMENT_SPEC);
    }

    @Test
    void ambiguousRequirementTriggersClarification() {
        AgentContext context = new AgentContext(UUID.randomUUID(), "requirements",
                "Make the URL shortener highly scalable and improve analytics performance.",
                Map.of(), Map.of());

        AgentResult result = agent.execute(context);

        assertThat(result.requiresClarification()).isTrue();
        assertThat(result.ambiguities()).isNotEmpty();
        assertThat(result.ambiguities()).anyMatch(a -> a.contains("scalable") || a.contains("availability"));
    }

    @Test
    void unambiguousBrownfieldRequirementDoesNotTriggerClarification() {
        AgentContext context = new AgentContext(UUID.randomUUID(), "requirements",
                "Add configurable URL expiration without breaking existing clients.",
                Map.of(), Map.of());

        AgentResult result = agent.execute(context);

        assertThat(result.requiresClarification()).isFalse();
    }
}
