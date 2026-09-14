package com.example.agentic.url;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "agentic.url")
public record UrlProperties(
        String baseDomain,
        int shortCodeLength,
        List<String> allowedSchemes,
        int maxUrlLength,
        int defaultExpiryDays
) {
}
