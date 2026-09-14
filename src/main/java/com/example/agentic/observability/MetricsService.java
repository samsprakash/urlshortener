package com.example.agentic.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer counters/timers backing 01-architecture.md §9's reliability
 * metrics: success rate, retry/rollback frequency, MTTR (approval.wait.time as
 * a proxy for human-in-the-loop latency), end-to-end latency.
 */
@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void workflowSucceeded() {
        Counter.builder("workflow.success").register(registry).increment();
    }

    public void workflowFailed() {
        Counter.builder("workflow.failure").register(registry).increment();
    }

    public void nodeRetried() {
        Counter.builder("workflow.retry").register(registry).increment();
    }

    public void rollbackExecuted() {
        Counter.builder("workflow.rollback").register(registry).increment();
    }

    public void replanExecuted() {
        Counter.builder("workflow.replan").register(registry).increment();
    }

    public void recordWorkflowDuration(Duration duration) {
        Timer.builder("workflow.duration").register(registry).record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void recordNodeDuration(String nodeKey, Duration duration) {
        Timer.builder("node.duration").tag("node", nodeKey).register(registry)
                .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void recordApprovalWaitTime(Duration duration) {
        Timer.builder("approval.wait.time").register(registry).record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }
}
