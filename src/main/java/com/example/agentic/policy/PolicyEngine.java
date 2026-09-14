package com.example.agentic.policy;

import com.example.agentic.agent.AgentAction;
import com.example.agentic.agent.AgentContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs every registered Policy against a proposed action and combines the
 * results with DENY > REQUIRE_APPROVAL > ALLOW precedence: the most
 * conservative outcome wins. This is the single choke point every agent
 * proposal passes through before the orchestrator commits anything (ADR-6).
 */
@Component
public class PolicyEngine {

    private final List<Policy> policies;

    public PolicyEngine(List<Policy> policies) {
        this.policies = policies;
    }

    public PolicyDecision evaluate(AgentAction action, AgentContext context) {
        PolicyDecision mostConservative = PolicyDecision.allow("No policies registered");
        for (Policy policy : policies) {
            PolicyDecision decision = policy.evaluate(action, context);
            if (decision.type() == DecisionType.DENY) {
                return decision;
            }
            if (decision.type() == DecisionType.REQUIRE_APPROVAL) {
                mostConservative = decision;
            } else if (mostConservative.type() == DecisionType.ALLOW) {
                mostConservative = decision;
            }
        }
        return mostConservative;
    }
}
