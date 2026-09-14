package com.example.agentic.url.security;

import com.example.agentic.url.UrlProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UrlValidatorTest {

    private final UrlProperties properties = new UrlProperties(
            "http://localhost:8080", 7, List.of("http", "https"), 2048, 365);
    private final UrlValidator validator = new UrlValidator(properties);

    @Test
    void acceptsHttpsUrl() {
        assertThat(validator.validate("https://example.com/path").valid()).isTrue();
    }

    @Test
    void acceptsHttpUrl() {
        assertThat(validator.validate("http://example.com").valid()).isTrue();
    }

    @Test
    void rejectsJavascriptScheme() {
        UrlValidator.ValidationResult result = validator.validate("javascript:alert(1)");
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("not allowed");
    }

    @Test
    void rejectsDataScheme() {
        assertThat(validator.validate("data:text/html,<script>alert(1)</script>").valid()).isFalse();
    }

    @Test
    void rejectsBlankUrl() {
        assertThat(validator.validate("").valid()).isFalse();
        assertThat(validator.validate(null).valid()).isFalse();
    }

    @Test
    void rejectsUrlExceedingMaxLength() {
        String longUrl = "https://example.com/" + "a".repeat(3000);
        assertThat(validator.validate(longUrl).valid()).isFalse();
    }

    @Test
    void rejectsUrlWithoutHost() {
        assertThat(validator.validate("https:///path").valid()).isFalse();
    }

    @Test
    void rejectsMalformedUrl() {
        assertThat(validator.validate("ht!tp://[invalid").valid()).isFalse();
    }
}
