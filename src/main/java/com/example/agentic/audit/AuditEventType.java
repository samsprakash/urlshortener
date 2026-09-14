package com.example.agentic.audit;

public final class AuditEventType {

    public static final String WORKFLOW_STARTED = "WORKFLOW_STARTED";
    public static final String WORKFLOW_COMPLETED = "WORKFLOW_COMPLETED";
    public static final String WORKFLOW_FAILED = "WORKFLOW_FAILED";
    public static final String NODE_STARTED = "NODE_STARTED";
    public static final String NODE_SUCCEEDED = "NODE_SUCCEEDED";
    public static final String NODE_FAILED = "NODE_FAILED";
    public static final String NODE_RETRY = "NODE_RETRY";
    public static final String NODE_FALLBACK = "NODE_FALLBACK";
    public static final String POLICY_DENIED = "POLICY_DENIED";
    public static final String APPROVAL_REQUESTED = "APPROVAL_REQUESTED";
    public static final String APPROVAL_GRANTED = "APPROVAL_GRANTED";
    public static final String APPROVAL_REJECTED = "APPROVAL_REJECTED";
    public static final String ROLLBACK_STARTED = "ROLLBACK_STARTED";
    public static final String ROLLBACK_COMPLETED = "ROLLBACK_COMPLETED";
    public static final String SAFE_STOP = "SAFE_STOP";
    public static final String WORKFLOW_RESUMED = "WORKFLOW_RESUMED";
    public static final String CLARIFICATION_REQUIRED = "CLARIFICATION_REQUIRED";
    public static final String REQUIREMENT_CLARIFIED = "REQUIREMENT_CLARIFIED";
    public static final String REPLAN_STARTED = "REPLAN_STARTED";
    public static final String REPLAN_COMPLETED = "REPLAN_COMPLETED";
    public static final String ARTIFACT_INVALIDATED = "ARTIFACT_INVALIDATED";

    private AuditEventType() {
    }
}
