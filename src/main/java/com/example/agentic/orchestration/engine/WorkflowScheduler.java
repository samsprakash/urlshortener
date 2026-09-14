package com.example.agentic.orchestration.engine;

import com.example.agentic.orchestration.repo.WorkflowRepository;
import com.example.agentic.orchestration.state.Workflow;
import com.example.agentic.orchestration.state.WorkflowStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background safety net: WorkflowEngine.start()/approve()/resume() already
 * drive a workflow to quiescence synchronously, but this tick exists so a
 * RUNNING workflow that isn't actively being poked by a caller (e.g. one whose
 * last state change came from another node in the same tick that didn't loop
 * back) never silently stalls.
 */
@Component
public class WorkflowScheduler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowScheduler.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowEngine workflowEngine;

    public WorkflowScheduler(WorkflowRepository workflowRepository, WorkflowEngine workflowEngine) {
        this.workflowRepository = workflowRepository;
        this.workflowEngine = workflowEngine;
    }

    @Scheduled(fixedDelayString = "${agentic.orchestrator.scheduler-tick-ms:200}")
    public void tick() {
        List<Workflow> running = workflowRepository.findAll().stream()
                .filter(w -> w.getStatus() == WorkflowStatus.RUNNING)
                .toList();
        for (Workflow workflow : running) {
            try {
                workflowEngine.runToQuiescence(workflow.getId());
            } catch (Exception e) {
                log.error("Scheduler tick failed for workflow {}", workflow.getId(), e);
            }
        }
    }
}
