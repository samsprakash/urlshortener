package com.example.agentic.orchestration.api;

import com.example.agentic.artifact.Decision;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DecisionResponse(UUID id, UUID artifactId, String decision, String rationale,
                                List<String> alternatives, String selected, Instant createdAt) {
    public static DecisionResponse from(Decision decision) {
        return new DecisionResponse(decision.getId(), decision.getArtifactId(), decision.getDecision(),
                decision.getRationale(), decision.getAlternatives(), decision.getSelected(), decision.getCreatedAt());
    }
}
