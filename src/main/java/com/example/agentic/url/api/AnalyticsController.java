package com.example.agentic.url.api;

import com.example.agentic.url.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/v1/urls/{shortCode}/analytics")
    public AnalyticsResponse analytics(@PathVariable String shortCode) {
        return AnalyticsResponse.from(analyticsService.summarize(shortCode));
    }
}
