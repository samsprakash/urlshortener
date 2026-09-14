package com.example.agentic.events;

import java.time.Instant;

public record DomainEvent(String type, Object payload, Instant occurredAt) {

    public static DomainEvent of(String type, Object payload) {
        return new DomainEvent(type, payload, Instant.now());
    }
}
