package com.example.agentic.events;

/**
 * Seam between synchronous processing (v1) and an async broker (e.g. Kafka, see
 * ADR-3 / Scenario 2b in 03-scenarios.md). Callers depend only on this interface;
 * swapping InProcessEventPublisher for a KafkaEventPublisher is a config change,
 * not a rewrite of RedirectService/ClickTrackingService.
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}
