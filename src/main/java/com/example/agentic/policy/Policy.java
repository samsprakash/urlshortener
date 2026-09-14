package com.example.agentic.policy;

import com.example.agentic.agent.AgentAction;
import com.example.agentic.agent.AgentContext;

/**
 * A guardrail evaluated against every proposed agent action before the
 * orchestrator commits it. Agents propose; policies (and the orchestrator that
 * runs them) authorize (ADR-6).
 */
public interface Policy {

    PolicyDecision evaluate(AgentAction action, AgentContext context);
}
