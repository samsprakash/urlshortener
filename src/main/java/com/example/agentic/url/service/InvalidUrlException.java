package com.example.agentic.url.service;

public class InvalidUrlException extends RuntimeException {
    public InvalidUrlException(String reason) {
        super(reason);
    }
}
