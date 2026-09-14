package com.example.agentic.url.service;

import com.example.agentic.events.DomainEvent;
import com.example.agentic.events.EventPublisher;
import com.example.agentic.url.domain.ClickEvent;
import com.example.agentic.url.repo.ClickEventRepository;
import com.example.agentic.url.security.VisitorHasher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ClickTrackingService {

    public static final String CLICK_RECORDED_EVENT = "click.recorded";

    private final ClickEventRepository clickEventRepository;
    private final VisitorHasher visitorHasher;
    private final EventPublisher eventPublisher;

    public ClickTrackingService(ClickEventRepository clickEventRepository, VisitorHasher visitorHasher,
                                 EventPublisher eventPublisher) {
        this.clickEventRepository = clickEventRepository;
        this.visitorHasher = visitorHasher;
        this.eventPublisher = eventPublisher;
    }

    public void recordClick(UUID urlId, String remoteAddr, String userAgent, String referrer) {
        String visitorHash = visitorHasher.hash(remoteAddr == null ? "unknown" : remoteAddr);
        ClickPayload payload = new ClickPayload(urlId, Instant.now(), visitorHash, userAgent, referrer);
        eventPublisher.publish(DomainEvent.of(CLICK_RECORDED_EVENT, payload));
    }

    @EventListener
    @Transactional
    public void onClickRecorded(DomainEvent event) {
        if (!CLICK_RECORDED_EVENT.equals(event.type())) {
            return;
        }
        ClickPayload payload = (ClickPayload) event.payload();
        ClickEvent entity = new ClickEvent(UUID.randomUUID(), payload.urlId(), payload.occurredAt(),
                payload.visitorHash(), payload.userAgent(), payload.referrer());
        clickEventRepository.save(entity);
    }

    public record ClickPayload(UUID urlId, Instant occurredAt, String visitorHash,
                                String userAgent, String referrer) {
    }
}
