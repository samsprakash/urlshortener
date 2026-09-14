package com.example.agentic.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default v1 implementation: publishes synchronously in-process via Spring's
 * ApplicationEventPublisher. No broker, no operational overhead — see ADR-3.
 */
@Component
public class InProcessEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InProcessEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public InProcessEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing in-process event type={}", event.type());
        applicationEventPublisher.publishEvent(event);
    }
}
