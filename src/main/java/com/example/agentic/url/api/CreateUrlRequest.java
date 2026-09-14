package com.example.agentic.url.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateUrlRequest(
        @NotBlank(message = "url must not be blank") String url,
        @Positive(message = "expiryDays must be positive") Integer expiryDays
) {
}
