package com.example.agentic.agent;

import com.example.agentic.policy.RiskLevel;

/**
 * A structured description of what an agent wants to do, passed through
 * PolicyEngine before the orchestrator commits any of the agent's proposed
 * artifacts/decisions. This is the seam ADR-6 refers to: the LLM's output never
 * executes directly, it becomes this record and is evaluated like any other
 * action.
 */
public record AgentAction(AgentType agentType, String nodeKey, RiskLevel declaredRisk, String description) {
}
