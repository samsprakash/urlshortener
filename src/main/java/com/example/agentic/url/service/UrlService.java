package com.example.agentic.url.service;

import com.example.agentic.url.UrlProperties;
import com.example.agentic.url.domain.Url;
import com.example.agentic.url.domain.UrlStatus;
import com.example.agentic.url.repo.UrlRepository;
import com.example.agentic.url.security.UrlValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UrlService {

    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final UrlRepository urlRepository;
    private final UrlValidator urlValidator;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlProperties properties;

    public UrlService(UrlRepository urlRepository, UrlValidator urlValidator,
                       ShortCodeGenerator shortCodeGenerator, UrlProperties properties) {
        this.urlRepository = urlRepository;
        this.urlValidator = urlValidator;
        this.shortCodeGenerator = shortCodeGenerator;
        this.properties = properties;
    }

    @Transactional
    public Url createShortUrl(String originalUrl, Integer expiryDaysOverride) {
        UrlValidator.ValidationResult validation = urlValidator.validate(originalUrl);
        if (!validation.valid()) {
            throw new InvalidUrlException(validation.reason());
        }

        String shortCode = generateUniqueShortCode();
        int expiryDays = expiryDaysOverride != null ? expiryDaysOverride : properties.defaultExpiryDays();
        Instant now = Instant.now();
        Instant expiresAt = expiryDays > 0 ? now.plus(expiryDays, ChronoUnit.DAYS) : null;

        Url url = new Url(UUID.randomUUID(), shortCode, originalUrl, UrlStatus.ACTIVE, now, expiresAt);
        return urlRepository.save(url);
    }

    @Transactional(readOnly = true)
    public Url getByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = shortCodeGenerator.generate(properties.shortCodeLength());
            if (!urlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique short code after "
                + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
