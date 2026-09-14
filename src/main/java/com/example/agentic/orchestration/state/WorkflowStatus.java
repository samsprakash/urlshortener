package com.example.agentic.orchestration.state;

public enum WorkflowStatus {
    RUNNING,
    CLARIFICATION_REQUIRED,
    WAITING_FOR_APPROVAL,
    SAFE_STOPPED,
    SUCCEEDED,
    FAILED
}
