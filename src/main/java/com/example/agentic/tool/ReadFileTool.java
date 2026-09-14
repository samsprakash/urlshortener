package com.example.agentic.tool;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class ReadFileTool implements EngineeringTool {

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String path = (String) parameters.get("path");
        if (path == null || path.isBlank()) {
            return Map.of("success", false, "error", "path parameter is required");
        }
        try {
            String content = Files.readString(Path.of(path));
            return Map.of("success", true, "content", content);
        } catch (IOException e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
