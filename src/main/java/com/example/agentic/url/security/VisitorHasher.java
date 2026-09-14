package com.example.agentic.url.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes visitor IP addresses with a server-side salt before persistence.
 * Raw IPs are never stored — see 01-architecture.md §10.
 */
@Component
public class VisitorHasher {

    private final String salt;

    public VisitorHasher(@Value("${agentic.visitor-hash.salt}") String salt) {
        this.salt = salt;
    }

    public String hash(String ipAddress) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ipAddress.getBytes(StandardCharsets.UTF_8));
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] result = digest.digest();
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
