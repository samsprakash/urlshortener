package com.example.agentic.orchestration.state;

public enum NodeStatus {
    PENDING,
    READY,
    RUNNING,
    WAITING_FOR_APPROVAL,
    RETRYING,
    SUCCEEDED,
    FAILED,
    BLOCKED,
    ROLLED_BACK,
    SAFE_STOPPED,
    INVALIDATED
}
