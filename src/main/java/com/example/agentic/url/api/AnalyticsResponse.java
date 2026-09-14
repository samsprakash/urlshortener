package com.example.agentic.url.api;

import com.example.agentic.url.domain.ClickEvent;
import com.example.agentic.url.service.AnalyticsService;

import java.time.Instant;
import java.util.List;

public record AnalyticsResponse(
        String shortCode,
        String originalUrl,
        long totalClicks,
        long uniqueVisitors,
        List<ClickEventResponse> recentClicks
) {
    public static AnalyticsResponse from(AnalyticsService.AnalyticsSummary summary) {
        List<ClickEventResponse> clicks = summary.recentClicks().stream()
                .map(ClickEventResponse::from)
                .toList();
        return new AnalyticsResponse(summary.shortCode(), summary.originalUrl(), summary.totalClicks(),
                summary.uniqueVisitors(), clicks);
    }

    public record ClickEventResponse(Instant occurredAt, String userAgent, String referrer) {
        static ClickEventResponse from(ClickEvent event) {
            return new ClickEventResponse(event.getOccurredAt(), event.getUserAgent(), event.getReferrer());
        }
    }
}
