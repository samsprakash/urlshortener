package com.example.agentic.orchestration.retry;

import com.example.agentic.agent.AgentType;
import com.example.agentic.orchestration.state.WorkflowNode;
import com.example.agentic.policy.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private final RetryPolicy retryPolicy = new RetryPolicy();

    private WorkflowNode newNode(int maxRetries) {
        return new WorkflowNode(UUID.randomUUID(), UUID.randomUUID(), "n", AgentType.TEST,
                RiskLevel.MEDIUM, maxRetries, List.of());
    }

    @Test
    void retriesTransientFailureWithinBudget() {
        WorkflowNode node = newNode(2);
        assertThat(retryPolicy.decide(node, FailureType.TRANSIENT)).isEqualTo(RetryDecision.RETRY);
    }

    @Test
    void fallsBackAfterRetryBudgetExhausted() {
        WorkflowNode node = newNode(2);
        node.markRetrying();
        node.markRetrying();
        assertThat(retryPolicy.decide(node, FailureType.TRANSIENT)).isEqualTo(RetryDecision.FALLBACK);
    }

    @Test
    void securityFailureNeverRetries() {
        WorkflowNode node = newNode(2);
        assertThat(retryPolicy.decide(node, FailureType.SECURITY)).isEqualTo(RetryDecision.FALLBACK);
    }

    @Test
    void validationFailureNeverRetries() {
        WorkflowNode node = newNode(2);
        assertThat(retryPolicy.decide(node, FailureType.VALIDATION)).isEqualTo(RetryDecision.FALLBACK);
    }

    @Test
    void infrastructureFailureRetries() {
        WorkflowNode node = newNode(2);
        assertThat(retryPolicy.decide(node, FailureType.INFRASTRUCTURE)).isEqualTo(RetryDecision.RETRY);
    }
}
