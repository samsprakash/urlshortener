package com.example.agentic.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/** Simulated diff summary tool — see RunTestTool for why this is simulated in the prototype. */
@Component
public class GitDiffTool implements EngineeringTool {

    @Override
    public String name() {
        return "git_diff";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String nodeKey = (String) parameters.getOrDefault("nodeKey", "unknown");
        return Map.of(
                "success", true,
                "filesChanged", 3,
                "insertions", 84,
                "deletions", 6,
                "summary", "Simulated diff for node " + nodeKey
        );
    }
}
