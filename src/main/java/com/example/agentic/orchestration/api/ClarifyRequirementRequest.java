package com.example.agentic.orchestration.api;

import jakarta.validation.constraints.NotBlank;

public record ClarifyRequirementRequest(@NotBlank String clarifiedRequirement) {
}
