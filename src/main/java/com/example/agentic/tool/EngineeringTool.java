package com.example.agentic.tool;

import java.util.Map;

/**
 * Allowlisted, parameterized tool boundary (01-architecture.md §10 /
 * "Prompt-injection posture"). Agents never get shell access; the orchestrator
 * invokes a specific named tool with specific structured parameters, never a
 * free-form command string assembled from LLM output.
 */
public interface EngineeringTool {

    String name();

    Map<String, Object> execute(Map<String, Object> parameters);
}
