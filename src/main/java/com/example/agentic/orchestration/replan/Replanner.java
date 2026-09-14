package com.example.agentic.orchestration.replan;

import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactStatus;
import com.example.agentic.artifact.repo.ArtifactRepository;
import com.example.agentic.audit.AuditEventType;
import com.example.agentic.audit.AuditOutcome;
import com.example.agentic.audit.AuditService;
import com.example.agentic.observability.MetricsService;
import com.example.agentic.orchestration.repo.WorkflowNodeRepository;
import com.example.agentic.orchestration.state.WorkflowNode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Applies an ImpactAnalyzer.ImpactResult: invalidates affected artifacts (kept,
 * not deleted, for lineage), resets affected nodes to PENDING for re-execution,
 * and leaves unaffected nodes/artifacts completely untouched — the concrete
 * mechanism behind "selective invalidation" in 01-architecture.md §8.
 */
@Component
public class Replanner {

    private final ArtifactRepository artifactRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final AuditService auditService;
    private final MetricsService metricsService;

    public Replanner(ArtifactRepository artifactRepository, WorkflowNodeRepository workflowNodeRepository,
                      AuditService auditService, MetricsService metricsService) {
        this.artifactRepository = artifactRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.auditService = auditService;
        this.metricsService = metricsService;
    }

    @Transactional
    public void replan(ImpactAnalyzer.ImpactResult impact, java.util.UUID workflowId, String correlationId) {
        auditService.record(workflowId, null, AuditEventType.REPLAN_STARTED, "orchestrator",
                AuditOutcome.PENDING, "Affected node count: " + impact.affectedNodes().size(), correlationId);

        List<Artifact> toInvalidate = artifactRepository.findByWorkflowIdAndStatus(workflowId, ArtifactStatus.ACTIVE)
                .stream()
                .filter(a -> impact.affectedArtifactTypes().contains(a.getType()))
                .toList();
        toInvalidate.forEach(Artifact::invalidate);
        artifactRepository.saveAll(toInvalidate);
        for (Artifact artifact : toInvalidate) {
            auditService.record(workflowId, artifact.getNodeId(), AuditEventType.ARTIFACT_INVALIDATED,
                    "orchestrator", AuditOutcome.SUCCESS,
                    artifact.getType() + " v" + artifact.getVersion() + " invalidated by re-plan", correlationId);
        }

        List<WorkflowNode> toReset = impact.affectedNodes();
        toReset.forEach(WorkflowNode::resetForReplan);
        workflowNodeRepository.saveAll(toReset);

        metricsService.replanExecuted();
        auditService.record(workflowId, null, AuditEventType.REPLAN_COMPLETED, "orchestrator",
                AuditOutcome.SUCCESS,
                toReset.size() + " node(s) reset, " + toInvalidate.size() + " artifact(s) invalidated, "
                        + impact.unaffectedNodes().size() + " node(s) preserved",
                correlationId);
    }
}
