package com.example.agentic.url.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "urls")
public class Url {

    @Id
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UrlStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    protected Url() {
        // JPA
    }

    public Url(UUID id, String shortCode, String originalUrl, UrlStatus status,
               Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = 0;
    }

    public boolean isUsable() {
        if (status != UrlStatus.ACTIVE) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public UUID getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public UrlStatus getStatus() {
        return status;
    }

    public void setStatus(UrlStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getClickCount() {
        return clickCount;
    }
}
