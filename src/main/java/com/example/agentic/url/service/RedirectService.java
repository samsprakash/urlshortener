package com.example.agentic.url.service;

import com.example.agentic.url.domain.Url;
import com.example.agentic.url.repo.UrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectService {

    private final UrlRepository urlRepository;
    private final ClickTrackingService clickTrackingService;

    public RedirectService(UrlRepository urlRepository, ClickTrackingService clickTrackingService) {
        this.urlRepository = urlRepository;
        this.clickTrackingService = clickTrackingService;
    }

    @Transactional
    public String resolve(String shortCode, String remoteAddr, String userAgent, String referrer) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (!url.isUsable()) {
            throw new UrlNotUsableException(shortCode, url.getStatus());
        }

        url.incrementClickCount();
        urlRepository.save(url);

        // Click tracking is fire-and-forget from the redirect's perspective: the seam is
        // EventPublisher, not a direct synchronous write, so this can later move to Kafka
        // (Scenario 2b) with zero change to this call site — see ADR-3.
        clickTrackingService.recordClick(url.getId(), remoteAddr, userAgent, referrer);

        return url.getOriginalUrl();
    }
}
