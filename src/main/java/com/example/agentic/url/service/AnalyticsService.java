package com.example.agentic.url.service;

import com.example.agentic.url.domain.ClickEvent;
import com.example.agentic.url.domain.Url;
import com.example.agentic.url.repo.ClickEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnalyticsService {

    private final UrlService urlService;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(UrlService urlService, ClickEventRepository clickEventRepository) {
        this.urlService = urlService;
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummary summarize(String shortCode) {
        Url url = urlService.getByShortCode(shortCode);
        long totalClicks = clickEventRepository.countByUrlId(url.getId());
        long uniqueVisitors = clickEventRepository.countDistinctVisitors(url.getId());
        List<ClickEvent> recent = clickEventRepository.findRecent(url.getId(),
                Instant.now().minus(30, ChronoUnit.DAYS));
        return new AnalyticsSummary(shortCode, url.getOriginalUrl(), totalClicks, uniqueVisitors,
                recent.stream().limit(50).toList());
    }

    public record AnalyticsSummary(String shortCode, String originalUrl, long totalClicks,
                                    long uniqueVisitors, List<ClickEvent> recentClicks) {
    }
}
