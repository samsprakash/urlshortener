package com.example.agentic.approval;

import com.example.agentic.audit.AuditEventType;
import com.example.agentic.audit.AuditOutcome;
import com.example.agentic.audit.AuditService;
import com.example.agentic.observability.MetricsService;
import com.example.agentic.orchestration.repo.WorkflowNodeRepository;
import com.example.agentic.orchestration.repo.WorkflowRepository;
import com.example.agentic.orchestration.state.NodeStatus;
import com.example.agentic.orchestration.state.Workflow;
import com.example.agentic.orchestration.state.WorkflowNode;
import com.example.agentic.orchestration.state.WorkflowStatus;
import com.example.agentic.policy.RiskLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowRepository workflowRepository;
    private final AuditService auditService;
    private final MetricsService metricsService;

    public ApprovalService(ApprovalRepository approvalRepository, WorkflowNodeRepository workflowNodeRepository,
                            WorkflowRepository workflowRepository, AuditService auditService,
                            MetricsService metricsService) {
        this.approvalRepository = approvalRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.workflowRepository = workflowRepository;
        this.auditService = auditService;
        this.metricsService = metricsService;
    }

    @Transactional
    public Approval request(WorkflowNode node, RiskLevel riskLevel, String actionDescription, String correlationId) {
        Approval approval = new Approval(UUID.randomUUID(), node.getWorkflowId(), node.getId(),
                actionDescription, riskLevel);
        approval = approvalRepository.save(approval);
        node.markWaitingForApproval();
        workflowNodeRepository.save(node);

        Workflow workflow = workflowRepository.findById(node.getWorkflowId()).orElseThrow();
        workflow.setStatus(WorkflowStatus.WAITING_FOR_APPROVAL);
        workflowRepository.save(workflow);

        auditService.record(node.getWorkflowId(), node.getId(), AuditEventType.APPROVAL_REQUESTED,
                "orchestrator", AuditOutcome.PENDING, actionDescription, correlationId);
        return approval;
    }

    @Transactional
    public Approval approve(UUID workflowId, UUID approvalId, String approver, String comment, String correlationId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new NoSuchElementException("No approval " + approvalId));
        requireBelongsToWorkflow(approval, workflowId);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval " + approvalId + " already decided: " + approval.getStatus());
        }
        metricsService.recordApprovalWaitTime(Duration.between(approval.getRequestedAt(), java.time.Instant.now()));
        approval.approve(approver, comment);
        approvalRepository.save(approval);

        WorkflowNode node = workflowNodeRepository.findById(approval.getNodeId()).orElseThrow();
        node.markReady();
        workflowNodeRepository.save(node);

        resumeWorkflowIfNoOtherPendingApprovals(approval.getWorkflowId());

        auditService.record(approval.getWorkflowId(), approval.getNodeId(), AuditEventType.APPROVAL_GRANTED,
                approver, AuditOutcome.SUCCESS, comment, correlationId);
        return approval;
    }

    @Transactional
    public Approval reject(UUID workflowId, UUID approvalId, String approver, String comment, String correlationId) {
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new NoSuchElementException("No approval " + approvalId));
        requireBelongsToWorkflow(approval, workflowId);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval " + approvalId + " already decided: " + approval.getStatus());
        }
        approval.reject(approver, comment);
        approvalRepository.save(approval);

        WorkflowNode node = workflowNodeRepository.findById(approval.getNodeId()).orElseThrow();
        node.markBlocked("Rejected by " + approver + (comment != null ? ": " + comment : ""));
        workflowNodeRepository.save(node);

        Workflow workflow = workflowRepository.findById(approval.getWorkflowId()).orElseThrow();
        workflow.setStatus(WorkflowStatus.FAILED);
        workflowRepository.save(workflow);

        auditService.record(approval.getWorkflowId(), approval.getNodeId(), AuditEventType.APPROVAL_REJECTED,
                approver, AuditOutcome.FAILURE, comment, correlationId);
        return approval;
    }

    @Transactional(readOnly = true)
    public List<Approval> listForWorkflow(UUID workflowId) {
        return approvalRepository.findByWorkflowIdOrderByRequestedAtAsc(workflowId);
    }

    /**
     * Guards against approving/rejecting a real approvalId through the wrong
     * workflow's URL path — without this, {workflowId} in the request path is
     * decorative (used only for the audit correlationId lookup) and any valid
     * approvalId can be actioned via any workflow's endpoint.
     */
    private void requireBelongsToWorkflow(Approval approval, UUID workflowId) {
        if (!approval.getWorkflowId().equals(workflowId)) {
            throw new NoSuchElementException(
                    "No approval " + approval.getId() + " on workflow " + workflowId);
        }
    }

    private void resumeWorkflowIfNoOtherPendingApprovals(UUID workflowId) {
        List<WorkflowNode> waiting = workflowNodeRepository.findByWorkflowIdAndStatus(
                workflowId, NodeStatus.WAITING_FOR_APPROVAL);
        if (waiting.isEmpty()) {
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
            if (workflow.getStatus() == WorkflowStatus.WAITING_FOR_APPROVAL) {
                workflow.setStatus(WorkflowStatus.RUNNING);
                workflowRepository.save(workflow);
            }
        }
    }
}
