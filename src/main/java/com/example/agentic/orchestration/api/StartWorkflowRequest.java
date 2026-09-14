package com.example.agentic.orchestration.api;

import com.example.agentic.orchestration.state.Scenario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record StartWorkflowRequest(
        @NotNull Scenario scenario,
        @NotBlank String requirement,
        Map<String, Object> metadata
) {
}
