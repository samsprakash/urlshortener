package com.example.agentic.policy;

public record PolicyDecision(DecisionType type, String reason, boolean requiresApproval) {

    public static PolicyDecision allow(String reason) {
        return new PolicyDecision(DecisionType.ALLOW, reason, false);
    }

    public static PolicyDecision deny(String reason) {
        return new PolicyDecision(DecisionType.DENY, reason, false);
    }

    public static PolicyDecision requireApproval(String reason) {
        return new PolicyDecision(DecisionType.REQUIRE_APPROVAL, reason, true);
    }
}
