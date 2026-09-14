package com.example.agentic.url.api;

import com.example.agentic.url.UrlProperties;
import com.example.agentic.url.domain.Url;
import com.example.agentic.url.service.RedirectService;
import com.example.agentic.url.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlController {

    private final UrlService urlService;
    private final RedirectService redirectService;
    private final UrlProperties properties;

    public UrlController(UrlService urlService, RedirectService redirectService, UrlProperties properties) {
        this.urlService = urlService;
        this.redirectService = redirectService;
        this.properties = properties;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        Url url = urlService.createShortUrl(request.url(), request.expiryDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(UrlResponse.from(url, properties));
    }

    @GetMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<UrlResponse> get(@PathVariable String shortCode) {
        Url url = urlService.getByShortCode(shortCode);
        return ResponseEntity.ok(UrlResponse.from(url, properties));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String target = redirectService.resolve(
                shortCode,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, target)
                .build();
    }
}
