package com.example.agentic.agent;

import java.util.List;

public record AgentResult(boolean success, AgentAction action, List<ProposedArtifact> artifacts,
                           List<ProposedDecision> decisions, List<String> ambiguities, String error) {

    public static AgentResult success(AgentAction action, List<ProposedArtifact> artifacts, List<ProposedDecision> decisions) {
        return new AgentResult(true, action, artifacts, decisions, List.of(), null);
    }

    public static AgentResult clarificationRequired(AgentAction action, List<String> ambiguities) {
        return new AgentResult(true, action, List.of(), List.of(), ambiguities, null);
    }

    public static AgentResult failure(AgentAction action, String error) {
        return new AgentResult(false, action, List.of(), List.of(), List.of(), error);
    }

    public boolean requiresClarification() {
        return !ambiguities.isEmpty();
    }
}
