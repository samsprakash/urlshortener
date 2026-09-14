package com.example.agentic.observability;

import java.util.Map;

public record MetricsSummaryResponse(
        long totalWorkflows,
        long succeededWorkflows,
        long failedWorkflows,
        double successRate,
        long totalRetries,
        long totalRollbacks,
        long totalReplans,
        Double avgApprovalWaitTimeMs,
        Double avgWorkflowDurationMs,
        Map<String, Long> nodeCountsByStatus
) {
}
