package com.example.agentic.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Simulated test runner for the prototype: the orchestrator's job is to prove
 * it governs a real dependency graph with real retry/rollback/approval
 * semantics, not to actually re-run this project's own Maven build as a side
 * effect of a demo API call. The "forceFailure" parameter is what the demo
 * script (03-scenarios.md step 5) uses to deterministically trigger the
 * retry -> fallback -> rollback path.
 */
@Component
public class RunTestTool implements EngineeringTool {

    @Override
    public String name() {
        return "run_test";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        boolean forceFailure = Boolean.TRUE.equals(parameters.get("forceFailure"));
        if (forceFailure) {
            return Map.of("success", false, "passed", false, "error", "Simulated test failure (forced by caller)");
        }
        return Map.of("success", true, "passed", true, "testsRun", 12, "testsFailed", 0);
    }
}
