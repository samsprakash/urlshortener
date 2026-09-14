package com.example.agentic.tool;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Writes are confined to a sandbox directory — this tool never accepts an
 * arbitrary absolute path from agent output, to keep the LLM boundary from
 * becoming an arbitrary-file-write primitive (01-architecture.md §10).
 */
@Component
public class WriteFileTool implements EngineeringTool {

    private static final Path SANDBOX_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "agentic-workspace");

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String relativePath = (String) parameters.get("path");
        String content = (String) parameters.get("content");
        if (relativePath == null || relativePath.isBlank() || content == null) {
            return Map.of("success", false, "error", "path and content parameters are required");
        }
        try {
            Path target = SANDBOX_ROOT.resolve(relativePath).normalize();
            if (!target.startsWith(SANDBOX_ROOT)) {
                return Map.of("success", false, "error", "path escapes sandbox root");
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
            return Map.of("success", true, "path", target.toString());
        } catch (IOException e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
