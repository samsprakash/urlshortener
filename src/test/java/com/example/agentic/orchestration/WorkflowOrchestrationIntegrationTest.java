package com.example.agentic.orchestration;

import com.example.agentic.AbstractIntegrationTest;
import com.example.agentic.approval.Approval;
import com.example.agentic.approval.ApprovalRepository;
import com.example.agentic.approval.ApprovalService;
import com.example.agentic.approval.ApprovalStatus;
import com.example.agentic.artifact.Decision;
import com.example.agentic.artifact.repo.DecisionRepository;
import com.example.agentic.audit.AuditEventRepository;
import com.example.agentic.audit.AuditEventType;
import com.example.agentic.orchestration.engine.WorkflowEngine;
import com.example.agentic.orchestration.graph.WorkflowGraphs;
import com.example.agentic.orchestration.repo.WorkflowNodeRepository;
import com.example.agentic.orchestration.repo.WorkflowRepository;
import com.example.agentic.orchestration.state.NodeStatus;
import com.example.agentic.orchestration.state.Scenario;
import com.example.agentic.orchestration.state.Workflow;
import com.example.agentic.orchestration.state.WorkflowNode;
import com.example.agentic.orchestration.state.WorkflowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class WorkflowOrchestrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WorkflowEngine workflowEngine;
    @Autowired
    private WorkflowNodeRepository workflowNodeRepository;
    @Autowired
    private ApprovalRepository approvalRepository;
    @Autowired
    private ApprovalService approvalService;
    @Autowired
    private DecisionRepository decisionRepository;
    @Autowired
    private AuditEventRepository auditEventRepository;
    @Autowired
    private WorkflowRepository workflowRepository;

    @Test
    void greenfieldRunsParallelBranchesAndParksAtReleaseApproval() {
        Workflow workflow = workflowEngine.start(Scenario.GREENFIELD,
                "Build a URL shortener that allows users to create short links and track clicks.",
                WorkflowGraphs.greenfield());

        List<WorkflowNode> nodes = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId());
        Map<String, NodeStatus> statusByKey = nodes.stream()
                .collect(java.util.stream.Collectors.toMap(WorkflowNode::getNodeKey, WorkflowNode::getStatus));

        // Everything up to and including documentation should have completed autonomously;
        // only the HIGH-risk release node should be gated on human approval.
        assertThat(statusByKey.get("requirements")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("architecture")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("planning")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("implementation")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("testing")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("security")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("documentation")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("release")).isEqualTo(NodeStatus.WAITING_FOR_APPROVAL);

        Workflow reloaded = reload(workflow.getId());
        assertThat(reloaded.getStatus()).isEqualTo(WorkflowStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void greenfieldRecordsBase62DecisionWithLineage() {
        Workflow workflow = workflowEngine.start(Scenario.GREENFIELD,
                "Build a URL shortener.", WorkflowGraphs.greenfield());

        List<Decision> decisions = decisionRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId());
        assertThat(decisions).anySatisfy(d -> {
            assertThat(d.getDecision()).contains("Base62");
            assertThat(d.getSelected()).isEqualTo("Base62");
            assertThat(d.getAlternatives()).contains("UUID", "Hashids");
        });
    }

    @Test
    void approvingReleaseCompletesWorkflowWithoutDuplicateApprovalRequests() {
        Workflow workflow = workflowEngine.start(Scenario.GREENFIELD,
                "Build a URL shortener.", WorkflowGraphs.greenfield());

        List<Approval> pending = approvalService.listForWorkflow(workflow.getId());
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getStatus()).isEqualTo(ApprovalStatus.PENDING);

        approvalService.approve(workflow.getId(), pending.get(0).getId(), "test-approver", "lgtm", workflow.getCorrelationId());
        workflowEngine.runToQuiescence(workflow.getId());

        await().atMost(java.time.Duration.ofSeconds(5)).untilAsserted(() -> {
            Workflow reloaded = reload(workflow.getId());
            assertThat(reloaded.getStatus()).isEqualTo(WorkflowStatus.SUCCEEDED);
        });

        // The approval-idempotency guard (WorkflowEngine's pendingApprovalResults stash)
        // must ensure exactly one approval was ever created for the release node.
        List<Approval> all = approvalService.listForWorkflow(workflow.getId());
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void rejectsApprovingAValidApprovalIdThroughTheWrongWorkflowsPath() {
        Workflow workflowA = workflowEngine.start(Scenario.GREENFIELD,
                "Workflow A for cross-workflow approval test.", WorkflowGraphs.greenfield());
        Workflow workflowB = workflowEngine.start(Scenario.GREENFIELD,
                "Workflow B for cross-workflow approval test.", WorkflowGraphs.greenfield());

        Approval approvalOnA = approvalService.listForWorkflow(workflowA.getId()).get(0);

        // approvalOnA is a real, PENDING approval id — but it belongs to workflow A, not B.
        assertThatThrownBy(() -> approvalService.approve(
                workflowB.getId(), approvalOnA.getId(), "attacker", "sneaky", workflowB.getCorrelationId()))
                .isInstanceOf(java.util.NoSuchElementException.class);

        // It must still be PENDING under its real workflow — the wrong-path call must not have touched it.
        Approval reloaded = approvalService.listForWorkflow(workflowA.getId()).get(0);
        assertThat(reloaded.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void brownfieldSchemaChangeRequiresApprovalAndPreservesOtherBranches() {
        Workflow workflow = workflowEngine.start(Scenario.BROWNFIELD,
                "Add configurable URL expiration without breaking existing clients.",
                WorkflowGraphs.brownfield());

        List<WorkflowNode> nodes = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId());
        Map<String, NodeStatus> statusByKey = nodes.stream()
                .collect(java.util.stream.Collectors.toMap(WorkflowNode::getNodeKey, WorkflowNode::getStatus));

        assertThat(statusByKey.get("schema")).isEqualTo(NodeStatus.WAITING_FOR_APPROVAL);
        // Independent branches not gated behind schema approval should have proceeded.
        assertThat(statusByKey.get("implementation")).isEqualTo(NodeStatus.SUCCEEDED);
        assertThat(statusByKey.get("test_plan")).isEqualTo(NodeStatus.SUCCEEDED);
        // regression_testing depends on schema (still pending) so it must not have run yet.
        assertThat(statusByKey.get("regression_testing")).isEqualTo(NodeStatus.PENDING);
    }

    @Test
    void ambiguousScenarioParksAtClarificationRequiredWithStructuredAmbiguities() {
        Workflow workflow = workflowEngine.start(Scenario.AMBIGUOUS,
                "Make the URL shortener highly scalable and improve analytics performance.",
                WorkflowGraphs.ambiguous());

        Workflow reloaded = reload(workflow.getId());
        assertThat(reloaded.getStatus()).isEqualTo(WorkflowStatus.CLARIFICATION_REQUIRED);

        WorkflowNode requirementsNode = workflowNodeRepository
                .findByWorkflowIdAndNodeKey(workflow.getId(), "requirements").orElseThrow();
        assertThat(requirementsNode.getStatus()).isEqualTo(NodeStatus.BLOCKED);
        assertThat(requirementsNode.getErrorMessage()).contains("Clarification required");

        assertThat(auditEventRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId()))
                .anyMatch(e -> e.getEventType().equals(AuditEventType.CLARIFICATION_REQUIRED));
    }

    @Test
    void clarifyingRequirementBumpsVersionAndExtendsGraph() {
        Workflow workflow = workflowEngine.start(Scenario.AMBIGUOUS,
                "Make the URL shortener highly scalable.", WorkflowGraphs.ambiguous());
        assertThat(reload(workflow.getId()).getStatus()).isEqualTo(WorkflowStatus.CLARIFICATION_REQUIRED);

        workflowEngine.clarifyRequirement(workflow.getId(),
                "Support 500 req/s sustained, p99 redirect latency under 100ms, 99.9% availability.",
                WorkflowGraphs.ambiguousResolved());

        Workflow reloaded = reload(workflow.getId());
        assertThat(reloaded.getRequirementVersion()).isEqualTo(2);

        List<WorkflowNode> nodes = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId());
        assertThat(nodes).extracting(WorkflowNode::getNodeKey)
                .contains("requirements", "architecture", "planning", "implementation",
                        "testing", "security", "documentation", "release");

        WorkflowNode requirementsNode = workflowNodeRepository
                .findByWorkflowIdAndNodeKey(workflow.getId(), "requirements").orElseThrow();
        assertThat(requirementsNode.getStatus()).isEqualTo(NodeStatus.SUCCEEDED);
    }

    @Test
    void forcedTestFailureExhaustsRetriesThenRollsBackAndBlocksDownstream() {
        Workflow workflow = workflowEngine.start(Scenario.GREENFIELD, "Build a URL shortener MVP.",
                WorkflowGraphs.greenfield(), Map.of("forceTestFailure", true));

        Workflow reloaded = reload(workflow.getId());
        assertThat(reloaded.getStatus()).isEqualTo(WorkflowStatus.FAILED);

        WorkflowNode testingNode = workflowNodeRepository
                .findByWorkflowIdAndNodeKey(workflow.getId(), "testing").orElseThrow();
        assertThat(testingNode.getStatus()).isEqualTo(NodeStatus.ROLLED_BACK);
        assertThat(testingNode.getRetryCount()).isEqualTo(testingNode.getMaxRetries());

        WorkflowNode documentationNode = workflowNodeRepository
                .findByWorkflowIdAndNodeKey(workflow.getId(), "documentation").orElseThrow();
        assertThat(documentationNode.getStatus()).isEqualTo(NodeStatus.BLOCKED);

        assertThat(auditEventRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId()))
                .extracting(e -> e.getEventType())
                .contains(AuditEventType.NODE_RETRY, AuditEventType.NODE_FALLBACK,
                        AuditEventType.ROLLBACK_STARTED, AuditEventType.ROLLBACK_COMPLETED,
                        AuditEventType.WORKFLOW_FAILED);
    }

    @Test
    void forcedSecurityFailureSkipsRetryAndRollsBackImmediately() {
        Workflow workflow = workflowEngine.start(Scenario.GREENFIELD, "Build a URL shortener MVP.",
                WorkflowGraphs.greenfield(), Map.of("forceSecurityFailure", true));

        WorkflowNode securityNode = workflowNodeRepository
                .findByWorkflowIdAndNodeKey(workflow.getId(), "security").orElseThrow();
        assertThat(securityNode.getStatus()).isEqualTo(NodeStatus.ROLLED_BACK);
        assertThat(securityNode.getRetryCount()).isEqualTo(0);
    }

    private Workflow reload(UUID id) {
        return workflowRepository.findById(id).orElseThrow();
    }
}
