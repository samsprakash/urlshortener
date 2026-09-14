package com.example.agentic.orchestration.retry;

import com.example.agentic.orchestration.state.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * Decides what happens after a node execution fails. Bounded retry
 * (maxRetries, default 2 per node) classified by FailureType — see
 * 01-architecture.md §6.
 */
@Component
public class RetryPolicy {

    public RetryDecision decide(WorkflowNode node, FailureType failureType) {
        if (!failureType.isRetryable()) {
            return RetryDecision.FALLBACK;
        }
        if (node.canRetry()) {
            return RetryDecision.RETRY;
        }
        return RetryDecision.FALLBACK;
    }
}
