package com.example.agentic.url.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Base62 short-code generation. Chosen over UUID (too long for a shortener) and
 * Hashids (adds a dependency for no real benefit here) — see the recorded
 * decision in the Greenfield scenario artifact lineage.
 */
@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
