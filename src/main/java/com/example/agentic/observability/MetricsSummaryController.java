package com.example.agentic.observability;

import com.example.agentic.orchestration.repo.WorkflowNodeRepository;
import com.example.agentic.orchestration.repo.WorkflowRepository;
import com.example.agentic.orchestration.state.NodeStatus;
import com.example.agentic.orchestration.state.WorkflowNode;
import com.example.agentic.orchestration.state.WorkflowStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSON summary satisfying 01-architecture.md §9's "success rate, retry/rollback
 * frequency, MTTR, end-to-end latency" requirement without requiring Grafana.
 */
@RestController
public class MetricsSummaryController {

    private final MeterRegistry meterRegistry;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;

    public MetricsSummaryController(MeterRegistry meterRegistry, WorkflowRepository workflowRepository,
                                     WorkflowNodeRepository workflowNodeRepository) {
        this.meterRegistry = meterRegistry;
        this.workflowRepository = workflowRepository;
        this.workflowNodeRepository = workflowNodeRepository;
    }

    @GetMapping("/api/v1/metrics/summary")
    public MetricsSummaryResponse summary() {
        long total = workflowRepository.count();
        long succeeded = workflowRepository.findAll().stream().filter(w -> w.getStatus() == WorkflowStatus.SUCCEEDED).count();
        long failed = workflowRepository.findAll().stream().filter(w -> w.getStatus() == WorkflowStatus.FAILED).count();
        double successRate = total == 0 ? 0.0 : (double) succeeded / total;

        long retries = counterValue("workflow.retry");
        long rollbacks = counterValue("workflow.rollback");
        long replans = counterValue("workflow.replan");

        Double avgApprovalWait = timerMean("approval.wait.time");
        Double avgWorkflowDuration = timerMean("workflow.duration");

        List<WorkflowNode> allNodes = workflowNodeRepository.findAll();
        Map<NodeStatus, Long> countsByStatus = new EnumMap<>(NodeStatus.class);
        for (WorkflowNode node : allNodes) {
            countsByStatus.merge(node.getStatus(), 1L, Long::sum);
        }
        Map<String, Long> nodeCounts = countsByStatus.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));

        return new MetricsSummaryResponse(total, succeeded, failed, successRate, retries, rollbacks, replans,
                avgApprovalWait, avgWorkflowDuration, nodeCounts);
    }

    private long counterValue(String name) {
        return Search.in(meterRegistry).name(name).counters().stream()
                .mapToLong(c -> (long) c.count())
                .sum();
    }

    private Double timerMean(String name) {
        List<Timer> timers = Search.in(meterRegistry).name(name).timers().stream().toList();
        if (timers.isEmpty()) {
            return null;
        }
        double totalMean = timers.stream().mapToDouble(t -> t.mean(java.util.concurrent.TimeUnit.MILLISECONDS)).sum();
        return totalMean / timers.size();
    }
}
