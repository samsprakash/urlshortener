package com.example.agentic.url.security;

import com.example.agentic.url.UrlProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Ingestion-time guardrails: scheme allowlist, length limits, and rejection of
 * non-http(s) schemes (javascript:, data:, file:, etc). See 01-architecture.md §10.
 */
@Component
public class UrlValidator {

    private final UrlProperties properties;

    public UrlValidator(UrlProperties properties) {
        this.properties = properties;
    }

    public ValidationResult validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return ValidationResult.rejected("URL must not be blank");
        }
        if (rawUrl.length() > properties.maxUrlLength()) {
            return ValidationResult.rejected("URL exceeds max length of " + properties.maxUrlLength());
        }

        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            return ValidationResult.rejected("URL is not syntactically valid: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            return ValidationResult.rejected("URL must specify a scheme");
        }
        if (properties.allowedSchemes().stream().noneMatch(s -> s.equalsIgnoreCase(scheme))) {
            return ValidationResult.rejected("Scheme '" + scheme + "' is not allowed; allowed: " + properties.allowedSchemes());
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            return ValidationResult.rejected("URL must specify a host");
        }

        return ValidationResult.accepted();
    }

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult accepted() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult rejected(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
