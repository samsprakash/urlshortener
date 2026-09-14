package com.example.agentic.agent;

import java.util.List;

public record ProposedDecision(String decision, String rationale, List<String> alternatives, String selected) {
}
