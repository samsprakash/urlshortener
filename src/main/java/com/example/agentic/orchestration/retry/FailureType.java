package com.example.agentic.orchestration.retry;

/**
 * Classification of why a node failed, per 01-architecture.md §6. Only
 * TRANSIENT/INFRASTRUCTURE auto-retry; SECURITY/VALIDATION go straight to
 * fallback or safe-stop because retrying won't change the outcome.
 */
public enum FailureType {
    TRANSIENT,
    VALIDATION,
    SECURITY,
    BUSINESS,
    INFRASTRUCTURE;

    public boolean isRetryable() {
        return this == TRANSIENT || this == INFRASTRUCTURE;
    }
}
