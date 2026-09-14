package com.example.agentic.agent;

/**
 * Agents propose; they never execute mutating actions directly (ADR-6). Every
 * implementation must be a pure function of its AgentContext — no direct DB
 * writes, no direct tool invocation. The orchestrator (WorkflowEngine) decides
 * whether/how to act on an AgentResult after PolicyEngine evaluates it.
 */
public interface Agent {

    AgentType type();

    AgentResult execute(AgentContext context);
}
