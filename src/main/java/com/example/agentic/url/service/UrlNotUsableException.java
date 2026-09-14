package com.example.agentic.url.service;

import com.example.agentic.url.domain.UrlStatus;

public class UrlNotUsableException extends RuntimeException {
    private final UrlStatus status;

    public UrlNotUsableException(String shortCode, UrlStatus status) {
        super("URL " + shortCode + " is not usable, status=" + status);
        this.status = status;
    }

    public UrlStatus getStatus() {
        return status;
    }
}
