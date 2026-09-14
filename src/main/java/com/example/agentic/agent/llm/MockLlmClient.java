package com.example.agentic.agent.llm;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Deterministic default (ADR-5). Agents in this system do not literally parse
 * LLM prose into decisions; they call this client to demonstrate the seam is
 * live and wired, and return a fixed acknowledgement. The actual reasoning
 * agents perform is deterministic Java so CI/demo runs are reproducible byte
 * for byte — the seam (LlmClient) is what a RealLlmClient would need to satisfy,
 * not the specific mock behavior.
 */
@Component
@Conditional(MockLlmModeCondition.class)
public class MockLlmClient implements LlmClient {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return "{\"mode\":\"mock\",\"acknowledged\":true}";
    }
}
