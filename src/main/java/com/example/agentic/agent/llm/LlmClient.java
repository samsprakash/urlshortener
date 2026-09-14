package com.example.agentic.agent.llm;

/**
 * Isolates agents from the concrete LLM provider (ADR-5). Every implementation
 * must return structured JSON conforming to the prompt's expected schema — the
 * orchestration domain never depends on raw LLM text.
 */
public interface LlmClient {

    /**
     * @param systemPrompt   role/behavior instructions
     * @param userPrompt     the task-specific prompt, including any context serialized as JSON
     * @return raw JSON text conforming to the schema described in the prompt
     */
    String complete(String systemPrompt, String userPrompt);
}
