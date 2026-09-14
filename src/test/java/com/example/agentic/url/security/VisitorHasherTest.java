package com.example.agentic.url.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorHasherTest {

    private final VisitorHasher hasher = new VisitorHasher("test-salt");

    @Test
    void producesSameHashForSameInput() {
        assertThat(hasher.hash("1.2.3.4")).isEqualTo(hasher.hash("1.2.3.4"));
    }

    @Test
    void producesDifferentHashesForDifferentIps() {
        assertThat(hasher.hash("1.2.3.4")).isNotEqualTo(hasher.hash("5.6.7.8"));
    }

    @Test
    void neverReturnsRawIp() {
        String hash = hasher.hash("1.2.3.4");
        assertThat(hash).doesNotContain("1.2.3.4");
        assertThat(hash).hasSize(64); // SHA-256 hex digest length
    }
}
