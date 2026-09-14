package com.example.agentic.orchestration.api;

import com.example.agentic.approval.Approval;
import com.example.agentic.approval.ApprovalService;
import com.example.agentic.artifact.repo.ArtifactRepository;
import com.example.agentic.artifact.repo.DecisionRepository;
import com.example.agentic.orchestration.engine.WorkflowEngine;
import com.example.agentic.orchestration.graph.WorkflowGraphs;
import com.example.agentic.orchestration.repo.WorkflowNodeRepository;
import com.example.agentic.orchestration.repo.WorkflowRepository;
import com.example.agentic.orchestration.state.Workflow;
import com.example.agentic.scenario.ScenarioRunner;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final ScenarioRunner scenarioRunner;
    private final WorkflowEngine workflowEngine;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final ArtifactRepository artifactRepository;
    private final DecisionRepository decisionRepository;
    private final ApprovalService approvalService;

    public WorkflowController(ScenarioRunner scenarioRunner, WorkflowEngine workflowEngine,
                               WorkflowRepository workflowRepository, WorkflowNodeRepository workflowNodeRepository,
                               ArtifactRepository artifactRepository, DecisionRepository decisionRepository,
                               ApprovalService approvalService) {
        this.scenarioRunner = scenarioRunner;
        this.workflowEngine = workflowEngine;
        this.workflowRepository = workflowRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.artifactRepository = artifactRepository;
        this.decisionRepository = decisionRepository;
        this.approvalService = approvalService;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> start(@Valid @RequestBody StartWorkflowRequest request) {
        Workflow workflow = scenarioRunner.run(request.scenario(), request.requirement(),
                request.metadata() == null ? Map.of() : request.metadata());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(workflow));
    }

    @GetMapping("/{id}")
    public WorkflowResponse get(@PathVariable UUID id) {
        return toResponse(findWorkflow(id));
    }

    @GetMapping("/{id}/artifacts")
    public List<ArtifactResponse> artifacts(@PathVariable UUID id) {
        return artifactRepository.findByWorkflowIdOrderByCreatedAtAsc(id).stream()
                .map(ArtifactResponse::from).toList();
    }

    @GetMapping("/{id}/decisions")
    public List<DecisionResponse> decisions(@PathVariable UUID id) {
        return decisionRepository.findByWorkflowIdOrderByCreatedAtAsc(id).stream()
                .map(DecisionResponse::from).toList();
    }

    @GetMapping("/{id}/approvals")
    public List<ApprovalResponse> approvals(@PathVariable UUID id) {
        return approvalService.listForWorkflow(id).stream().map(ApprovalResponse::from).toList();
    }

    @PostMapping("/{id}/approvals/{approvalId}/approve")
    public ApprovalResponse approve(@PathVariable UUID id, @PathVariable UUID approvalId,
                                     @RequestBody(required = false) ApprovalDecisionRequest request) {
        Workflow workflow = findWorkflow(id);
        String approver = request == null || request.approver() == null ? "demo-approver" : request.approver();
        String comment = request == null ? null : request.comment();
        Approval approval = approvalService.approve(id, approvalId, approver, comment, workflow.getCorrelationId());
        workflowEngine.runToQuiescence(id);
        return ApprovalResponse.from(approval);
    }

    @PostMapping("/{id}/approvals/{approvalId}/reject")
    public ApprovalResponse reject(@PathVariable UUID id, @PathVariable UUID approvalId,
                                    @RequestBody(required = false) ApprovalDecisionRequest request) {
        Workflow workflow = findWorkflow(id);
        String approver = request == null || request.approver() == null ? "demo-approver" : request.approver();
        String comment = request == null ? null : request.comment();
        Approval approval = approvalService.reject(id, approvalId, approver, comment, workflow.getCorrelationId());
        return ApprovalResponse.from(approval);
    }

    @PostMapping("/{id}/stop")
    public WorkflowResponse stop(@PathVariable UUID id) {
        workflowEngine.safeStop(id, "Manually stopped via API");
        return toResponse(findWorkflow(id));
    }

    @PostMapping("/{id}/resume")
    public WorkflowResponse resume(@PathVariable UUID id) {
        workflowEngine.resume(id);
        return toResponse(findWorkflow(id));
    }

    @PostMapping("/{id}/clarify")
    public WorkflowResponse clarify(@PathVariable UUID id, @Valid @RequestBody ClarifyRequirementRequest request) {
        workflowEngine.clarifyRequirement(id, request.clarifiedRequirement(), WorkflowGraphs.ambiguousResolved());
        return toResponse(findWorkflow(id));
    }

    private Workflow findWorkflow(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No workflow " + id));
    }

    private WorkflowResponse toResponse(Workflow workflow) {
        return WorkflowResponse.from(workflow, workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId()));
    }
}
