package com.example.agentic.url.api;

import com.example.agentic.url.domain.Url;
import com.example.agentic.url.UrlProperties;

import java.time.Instant;
import java.util.UUID;

public record UrlResponse(
        UUID id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        String status,
        Instant createdAt,
        Instant expiresAt,
        long clickCount
) {
    public static UrlResponse from(Url url, UrlProperties properties) {
        return new UrlResponse(
                url.getId(),
                url.getShortCode(),
                properties.baseDomain() + "/" + url.getShortCode(),
                url.getOriginalUrl(),
                url.getStatus().name(),
                url.getCreatedAt(),
                url.getExpiresAt(),
                url.getClickCount()
        );
    }
}
