package com.example.agentic.orchestration.rollback;

import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.repo.ArtifactRepository;
import com.example.agentic.audit.AuditEventType;
import com.example.agentic.audit.AuditOutcome;
import com.example.agentic.audit.AuditService;
import com.example.agentic.observability.MetricsService;
import com.example.agentic.orchestration.repo.WorkflowNodeRepository;
import com.example.agentic.orchestration.state.NodeStatus;
import com.example.agentic.orchestration.state.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Executes compensating actions for a failed node: invalidate the artifacts it
 * produced, and reset any already-succeeded downstream nodes so they re-run
 * against the corrected state after a re-plan (ADR-7).
 */
@Component
public class RollbackManager {

    private static final Logger log = LoggerFactory.getLogger(RollbackManager.class);

    private final ArtifactRepository artifactRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final AuditService auditService;
    private final MetricsService metricsService;

    public RollbackManager(ArtifactRepository artifactRepository, WorkflowNodeRepository workflowNodeRepository,
                            AuditService auditService, MetricsService metricsService) {
        this.artifactRepository = artifactRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.auditService = auditService;
        this.metricsService = metricsService;
    }

    @Transactional
    public void rollbackNode(WorkflowNode failedNode, String reason, String correlationId) {
        auditService.record(failedNode.getWorkflowId(), failedNode.getId(), AuditEventType.ROLLBACK_STARTED,
                "orchestrator", AuditOutcome.PENDING, reason, correlationId);

        List<RollbackAction> actions = new ArrayList<>();

        List<Artifact> producedArtifacts = artifactRepository.findByWorkflowIdOrderByCreatedAtAsc(failedNode.getWorkflowId())
                .stream()
                .filter(a -> failedNode.getId().equals(a.getNodeId()))
                .toList();
        for (Artifact artifact : producedArtifacts) {
            actions.add(new InvalidateArtifactRollback(artifact));
        }

        List<WorkflowNode> downstream = findTransitiveDownstream(failedNode);
        for (WorkflowNode node : downstream) {
            actions.add(new ResetNodeRollback(node));
        }

        for (RollbackAction action : actions) {
            log.info("Rollback compensating action: {}", action.description());
            action.compensate();
        }

        artifactRepository.saveAll(producedArtifacts);
        workflowNodeRepository.saveAll(downstream);

        failedNode.markRolledBack();
        workflowNodeRepository.save(failedNode);

        metricsService.rollbackExecuted();
        auditService.record(failedNode.getWorkflowId(), failedNode.getId(), AuditEventType.ROLLBACK_COMPLETED,
                "orchestrator", AuditOutcome.SUCCESS,
                actions.size() + " compensating action(s) applied", correlationId);
    }

    private List<WorkflowNode> findTransitiveDownstream(WorkflowNode failedNode) {
        List<WorkflowNode> all = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(failedNode.getWorkflowId());
        Set<String> affected = new java.util.HashSet<>();
        affected.add(failedNode.getNodeKey());

        boolean changed = true;
        while (changed) {
            changed = false;
            for (WorkflowNode node : all) {
                if (affected.contains(node.getNodeKey())) {
                    continue;
                }
                if (node.getDependsOn().stream().anyMatch(affected::contains)) {
                    affected.add(node.getNodeKey());
                    changed = true;
                }
            }
        }
        affected.remove(failedNode.getNodeKey());

        return all.stream()
                .filter(n -> affected.contains(n.getNodeKey()))
                .filter(n -> n.getStatus() == NodeStatus.SUCCEEDED)
                .toList();
    }
}
