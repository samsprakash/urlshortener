package com.example.agentic.url.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    private UUID id;

    @Column(name = "url_id", nullable = false)
    private UUID urlId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "visitor_hash", nullable = false, length = 64)
    private String visitorHash;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "referrer", length = 1024)
    private String referrer;

    protected ClickEvent() {
        // JPA
    }

    public ClickEvent(UUID id, UUID urlId, Instant occurredAt, String visitorHash,
                       String userAgent, String referrer) {
        this.id = id;
        this.urlId = urlId;
        this.occurredAt = occurredAt;
        this.visitorHash = visitorHash;
        this.userAgent = userAgent;
        this.referrer = referrer;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUrlId() {
        return urlId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getVisitorHash() {
        return visitorHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }
}
