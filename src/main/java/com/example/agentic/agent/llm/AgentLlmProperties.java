package com.example.agentic.agent.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentic.agent")
public record AgentLlmProperties(Llm llm, Real real) {

    public record Llm(String mode) {
    }

    public record Real(String provider, String model, String apiKey) {
    }
}
