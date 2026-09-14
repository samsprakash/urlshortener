package com.example.agentic.orchestration.engine;

import com.example.agentic.agent.*;
import com.example.agentic.approval.ApprovalService;
import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactType;
import com.example.agentic.artifact.Decision;
import com.example.agentic.artifact.repo.ArtifactRepository;
import com.example.agentic.artifact.repo.DecisionRepository;
import com.example.agentic.audit.AuditEventType;
import com.example.agentic.audit.AuditOutcome;
import com.example.agentic.audit.AuditService;
import com.example.agentic.observability.MetricsService;
import com.example.agentic.orchestration.graph.DependencyResolver;
import com.example.agentic.orchestration.graph.GraphNodeSpec;
import com.example.agentic.orchestration.graph.WorkflowGraph;
import com.example.agentic.orchestration.repo.WorkflowNodeRepository;
import com.example.agentic.orchestration.repo.WorkflowRepository;
import com.example.agentic.orchestration.replan.ImpactAnalyzer;
import com.example.agentic.orchestration.replan.Replanner;
import com.example.agentic.orchestration.retry.FailureType;
import com.example.agentic.orchestration.retry.RetryDecision;
import com.example.agentic.orchestration.retry.RetryPolicy;
import com.example.agentic.orchestration.rollback.RollbackManager;
import com.example.agentic.orchestration.state.*;
import com.example.agentic.policy.PolicyDecision;
import com.example.agentic.policy.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The brain (01-architecture.md §1): for every tick, evaluates dependencies,
 * policy, approval state, and safe-stop status before moving any node
 * READY -> RUNNING. Agents only ever propose; this class is the only thing
 * that persists workflow/node/artifact/decision state (ADR-6).
 */
@Component
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final ArtifactRepository artifactRepository;
    private final DecisionRepository decisionRepository;
    private final AgentRegistry agentRegistry;
    private final PolicyEngine policyEngine;
    private final ApprovalService approvalService;
    private final RetryPolicy retryPolicy;
    private final RollbackManager rollbackManager;
    private final ImpactAnalyzer impactAnalyzer;
    private final Replanner replanner;
    private final AuditService auditService;
    private final MetricsService metricsService;
    private final WorkflowRuntimeMetadata runtimeMetadata;

    private final Set<UUID> safeStoppedWorkflows = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /**
     * Guards against the background WorkflowScheduler tick and a synchronous
     * caller (start/approve/resume) driving the same workflow's scheduling loop
     * concurrently — without this, both can observe the same READY node before
     * either commits, and execute (and re-request approval for) it twice.
     * Single-process-only, matching this prototype's "modular monolith" scope
     * (ADR-4); a multi-instance deployment would need a DB-level advisory lock
     * instead.
     */
    private final Map<UUID, java.util.concurrent.locks.ReentrantLock> workflowLocks = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Holds the already-computed AgentResult for a node parked at
     * WAITING_FOR_APPROVAL, keyed by node id. Approval authorizes committing
     * this proposal — it must not cause the agent to re-run or the policy
     * engine to re-evaluate, both of which are idempotent-unsafe here (e.g. a
     * HIGH-risk node would otherwise re-trigger REQUIRE_APPROVAL forever).
     */
    private final Map<UUID, PendingExecution> pendingApprovalResults = new java.util.concurrent.ConcurrentHashMap<>();

    private record PendingExecution(AgentResult result, Instant start) {
    }

    /** Self-reference through the Spring proxy so tick()/finalizeIfComplete() get real transactions even on self-invocation. */
    @Autowired
    @Lazy
    private WorkflowEngine self;

    public WorkflowEngine(WorkflowRepository workflowRepository, WorkflowNodeRepository workflowNodeRepository,
                           ArtifactRepository artifactRepository, DecisionRepository decisionRepository,
                           AgentRegistry agentRegistry, PolicyEngine policyEngine, ApprovalService approvalService,
                           RetryPolicy retryPolicy, RollbackManager rollbackManager, ImpactAnalyzer impactAnalyzer,
                           Replanner replanner, AuditService auditService, MetricsService metricsService,
                           WorkflowRuntimeMetadata runtimeMetadata) {
        this.workflowRepository = workflowRepository;
        this.workflowNodeRepository = workflowNodeRepository;
        this.artifactRepository = artifactRepository;
        this.decisionRepository = decisionRepository;
        this.agentRegistry = agentRegistry;
        this.policyEngine = policyEngine;
        this.approvalService = approvalService;
        this.retryPolicy = retryPolicy;
        this.rollbackManager = rollbackManager;
        this.impactAnalyzer = impactAnalyzer;
        this.replanner = replanner;
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.runtimeMetadata = runtimeMetadata;
    }

    public Workflow start(Scenario scenario, String requirementText, WorkflowGraph graph) {
        return start(scenario, requirementText, graph, Map.of());
    }

    @Transactional
    public Workflow start(Scenario scenario, String requirementText, WorkflowGraph graph, Map<String, Object> metadata) {
        Workflow workflow = new Workflow(UUID.randomUUID(), scenario, requirementText,
                "corr-" + UUID.randomUUID());
        workflow = workflowRepository.save(workflow);
        runtimeMetadata.put(workflow.getId(), metadata);

        materializeGraph(workflow.getId(), graph);

        auditService.record(workflow.getId(), null, AuditEventType.WORKFLOW_STARTED, "orchestrator",
                AuditOutcome.SUCCESS, "scenario=" + scenario, workflow.getCorrelationId());

        runToQuiescence(workflow.getId());
        return workflowRepository.findById(workflow.getId()).orElseThrow();
    }

    private void materializeGraph(UUID workflowId, WorkflowGraph graph) {
        for (GraphNodeSpec spec : graph.nodes()) {
            WorkflowNode node = new WorkflowNode(UUID.randomUUID(), workflowId, spec.nodeKey(), spec.agentType(),
                    spec.riskLevel(), spec.maxRetries(), spec.dependsOnList());
            workflowNodeRepository.save(node);
        }
    }

    /**
     * Adds new node specs (from a re-plan or scenario continuation) to an
     * existing workflow without disturbing nodes that already exist.
     */
    @Transactional
    public void extendGraph(UUID workflowId, WorkflowGraph graph) {
        List<WorkflowNode> existing = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
        Set<String> existingKeys = existing.stream().map(WorkflowNode::getNodeKey).collect(Collectors.toSet());
        for (GraphNodeSpec spec : graph.nodes()) {
            if (!existingKeys.contains(spec.nodeKey())) {
                WorkflowNode node = new WorkflowNode(UUID.randomUUID(), workflowId, spec.nodeKey(), spec.agentType(),
                        spec.riskLevel(), spec.maxRetries(), spec.dependsOnList());
                workflowNodeRepository.save(node);
            }
        }
    }

    /**
     * Drives the scheduler loop until no more progress can be made in this
     * call (either everything terminal, or blocked on approval/safe-stop).
     * Called synchronously after start()/approve()/resume() so demo callers see
     * a settled state; a background @Scheduled tick (WorkflowScheduler) also
     * calls this for workflows that are waiting on external async completion.
     */
    public void runToQuiescence(UUID workflowId) {
        if (safeStoppedWorkflows.contains(workflowId)) {
            return;
        }
        java.util.concurrent.locks.ReentrantLock lock = workflowLocks.computeIfAbsent(
                workflowId, id -> new java.util.concurrent.locks.ReentrantLock());
        if (!lock.tryLock()) {
            // Another thread (scheduler tick or a concurrent API call) is already
            // driving this workflow; it will reach quiescence on its own.
            return;
        }
        try {
            boolean progressed = true;
            int guard = 0;
            while (progressed && guard++ < 500) {
                progressed = self.tick(workflowId);
            }
            self.finalizeIfComplete(workflowId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * One scheduling pass: find all READY-eligible PENDING nodes, execute them
     * (parallel-eligible nodes run within this same pass since there's no
     * cross-node ordering constraint among them), and report whether any
     * progress was made.
     */
    @Transactional
    public boolean tick(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        if (workflow.getStatus() == WorkflowStatus.SAFE_STOPPED
                || workflow.getStatus() == WorkflowStatus.WAITING_FOR_APPROVAL
                || workflow.getStatus() == WorkflowStatus.CLARIFICATION_REQUIRED
                || workflow.getStatus() == WorkflowStatus.SUCCEEDED
                || workflow.getStatus() == WorkflowStatus.FAILED) {
            return false;
        }

        List<WorkflowNode> allNodes = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
        Map<String, WorkflowNode> byKey = allNodes.stream().collect(Collectors.toMap(WorkflowNode::getNodeKey, n -> n));

        List<WorkflowNode> readyNodes = allNodes.stream()
                .filter(n -> n.getStatus() == NodeStatus.READY || DependencyResolver.isReady(n, byKey))
                .toList();

        boolean progressed = false;
        for (WorkflowNode node : readyNodes) {
            PendingExecution pending = pendingApprovalResults.remove(node.getId());
            if (pending != null) {
                commitSuccess(workflow, node, pending.result(), pending.start());
            } else {
                executeNode(workflow, node);
            }
            progressed = true;
        }

        for (WorkflowNode node : allNodes) {
            if (node.getStatus() == NodeStatus.PENDING && DependencyResolver.isBlockedByFailedDependency(node, byKey)) {
                node.markBlocked("Upstream dependency failed/blocked/rolled back");
                workflowNodeRepository.save(node);
                progressed = true;
            }
        }

        return progressed;
    }

    private void executeNode(Workflow workflow, WorkflowNode node) {
        node.markRunning();
        workflowNodeRepository.save(node);
        auditService.record(workflow.getId(), node.getId(), AuditEventType.NODE_STARTED, "orchestrator",
                AuditOutcome.PENDING, node.getAgentType().name(), workflow.getCorrelationId());

        Instant start = Instant.now();
        Agent agent = agentRegistry.get(node.getAgentType());
        AgentContext context = buildContext(workflow, node);
        AgentResult result = agent.execute(context);

        if (result.requiresClarification()) {
            handleClarificationRequired(workflow, node, result);
            return;
        }

        if (!result.success()) {
            handleAgentFailure(workflow, node, result, start);
            return;
        }

        PolicyDecision policyDecision = policyEngine.evaluate(result.action(), context);
        switch (policyDecision.type()) {
            case DENY -> handlePolicyDenied(workflow, node, policyDecision);
            case REQUIRE_APPROVAL -> {
                pendingApprovalResults.put(node.getId(), new PendingExecution(result, start));
                approvalService.request(node, node.getRiskLevel(), result.action().description(), workflow.getCorrelationId());
            }
            case ALLOW -> commitSuccess(workflow, node, result, start);
        }
    }

    private AgentContext buildContext(Workflow workflow, WorkflowNode node) {
        List<Artifact> artifacts = artifactRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId());
        Map<ArtifactType, Artifact> latestByType = new EnumMap<>(ArtifactType.class);
        for (Artifact artifact : artifacts) {
            if (artifact.getStatus() == com.example.agentic.artifact.ArtifactStatus.ACTIVE) {
                latestByType.merge(artifact.getType(), artifact, (a, b) -> a.getVersion() >= b.getVersion() ? a : b);
            }
        }
        return new AgentContext(workflow.getId(), node.getNodeKey(), workflow.getRequirementText(),
                latestByType, runtimeMetadata.get(workflow.getId()));
    }

    private void commitSuccess(Workflow workflow, WorkflowNode node, AgentResult result, Instant start) {
        Map<String, Integer> versions = new LinkedHashMap<>();
        for (ProposedArtifact proposed : result.artifacts()) {
            int nextVersion = artifactRepository.findByWorkflowIdAndTypeOrderByVersionDesc(workflow.getId(), proposed.type())
                    .stream().findFirst().map(a -> a.getVersion() + 1).orElse(1);
            Artifact artifact = new Artifact(UUID.randomUUID(), workflow.getId(), node.getId(), proposed.type(),
                    nextVersion, proposed.content(), proposed.sourceArtifactIds(), node.getAgentType().name());
            artifact = artifactRepository.save(artifact);
            versions.put(proposed.type().name(), nextVersion);

            for (ProposedDecision decision : result.decisions()) {
                Decision entity = new Decision(UUID.randomUUID(), workflow.getId(), artifact.getId(),
                        decision.decision(), decision.rationale(), decision.alternatives(), decision.selected());
                decisionRepository.save(entity);
            }
        }

        node.markSucceeded();
        workflowNodeRepository.save(node);
        metricsService.recordNodeDuration(node.getNodeKey(), Duration.between(start, Instant.now()));
        auditService.record(workflow.getId(), node.getId(), AuditEventType.NODE_SUCCEEDED, "orchestrator",
                AuditOutcome.SUCCESS, null, workflow.getCorrelationId(), versions);
    }

    private void handleAgentFailure(Workflow workflow, WorkflowNode node, AgentResult result, Instant start) {
        FailureType failureType = classifyFailure(node);
        RetryDecision decision = retryPolicy.decide(node, failureType);

        metricsService.recordNodeDuration(node.getNodeKey(), Duration.between(start, Instant.now()));

        switch (decision) {
            case RETRY -> {
                node.markRetrying();
                workflowNodeRepository.save(node);
                metricsService.nodeRetried();
                auditService.record(workflow.getId(), node.getId(), AuditEventType.NODE_RETRY, "orchestrator",
                        AuditOutcome.FAILURE, result.error(), workflow.getCorrelationId());
                node.markReady();
                workflowNodeRepository.save(node);
            }
            case FALLBACK -> {
                node.markFailed(result.error());
                workflowNodeRepository.save(node);
                auditService.record(workflow.getId(), node.getId(), AuditEventType.NODE_FALLBACK, "orchestrator",
                        AuditOutcome.FAILURE, result.error(), workflow.getCorrelationId());
                // Compensating rollback for this node and its downstream successors, and
                // stop here: rollback is a terminal outcome for this run, not an automatic
                // trigger for a full requirement-change replan (Replanner is reserved for
                // the explicit clarifyRequirement() flow in 01-architecture.md §8). A human
                // can inspect the audit trail and decide whether to fix and retry, or feed
                // a revised requirement through the clarify endpoint.
                rollbackManager.rollbackNode(node, "Retry exhausted: " + result.error(), workflow.getCorrelationId());
            }
            case SAFE_STOP -> safeStop(workflow.getId(), "Non-retryable failure type " + failureType + " on node "
                    + node.getNodeKey() + ": " + result.error());
        }
    }

    private FailureType classifyFailure(WorkflowNode node) {
        return switch (node.getAgentType()) {
            case SECURITY -> FailureType.SECURITY;
            case TEST -> FailureType.TRANSIENT;
            default -> FailureType.TRANSIENT;
        };
    }

    private void handlePolicyDenied(Workflow workflow, WorkflowNode node, PolicyDecision decision) {
        node.markBlocked("Policy denied: " + decision.reason());
        workflowNodeRepository.save(node);
        auditService.record(workflow.getId(), node.getId(), AuditEventType.POLICY_DENIED, "policy-engine",
                AuditOutcome.DENIED, decision.reason(), workflow.getCorrelationId());
        workflow.setStatus(WorkflowStatus.FAILED);
        workflowRepository.save(workflow);
    }

    private void handleClarificationRequired(Workflow workflow, WorkflowNode node, AgentResult result) {
        node.markBlocked("Clarification required: " + result.ambiguities());
        workflowNodeRepository.save(node);
        workflow.setStatus(WorkflowStatus.CLARIFICATION_REQUIRED);
        workflowRepository.save(workflow);
        auditService.record(workflow.getId(), node.getId(), AuditEventType.CLARIFICATION_REQUIRED, "orchestrator",
                AuditOutcome.PENDING, String.join("; ", result.ambiguities()), workflow.getCorrelationId());
    }

    @Transactional
    public void safeStop(UUID workflowId, String reason) {
        safeStoppedWorkflows.add(workflowId);
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        workflow.setStatus(WorkflowStatus.SAFE_STOPPED);
        workflowRepository.save(workflow);
        auditService.record(workflowId, null, AuditEventType.SAFE_STOP, "orchestrator",
                AuditOutcome.FAILURE, reason, workflow.getCorrelationId());
        log.warn("Workflow {} safe-stopped: {}", workflowId, reason);
    }

    /**
     * Scenario 3 (Ambiguous): feeds a clarified requirement back into a
     * CLARIFICATION_REQUIRED workflow, bumps the requirement version, resets
     * the requirements node so it re-runs against the clarified text, extends
     * the graph with the resolved shape (past the requirements gate), and
     * resumes scheduling (01-architecture.md §8).
     */
    @Transactional
    public Workflow clarifyRequirement(UUID workflowId, String clarifiedRequirementText, WorkflowGraph resolvedGraph) {
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        if (workflow.getStatus() != WorkflowStatus.CLARIFICATION_REQUIRED) {
            throw new IllegalStateException("Workflow " + workflowId + " is not awaiting clarification: " + workflow.getStatus());
        }
        workflow.bumpRequirementVersion(clarifiedRequirementText);
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflowRepository.save(workflow);

        WorkflowNode requirementsNode = workflowNodeRepository.findByWorkflowIdAndNodeKey(workflowId, "requirements")
                .orElseThrow();
        requirementsNode.resetForReplan();
        workflowNodeRepository.save(requirementsNode);

        extendGraph(workflowId, resolvedGraph);

        auditService.record(workflowId, null, AuditEventType.REQUIREMENT_CLARIFIED, "human", AuditOutcome.SUCCESS,
                clarifiedRequirementText, workflow.getCorrelationId());

        // Reuse the same ImpactAnalyzer/Replanner mechanism as Brownfield's
        // impact analysis (03-scenarios.md, Scenario 3) to selectively invalidate
        // any artifacts/nodes downstream of REQUIREMENT_SPEC. For a workflow that
        // parked before producing anything beyond the requirement, this is a
        // no-op; it is not a no-op for a requirement change arriving after
        // downstream work already exists.
        List<WorkflowNode> allNodes = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
        ImpactAnalyzer.ImpactResult impact = impactAnalyzer.analyzeRequirementChange(allNodes);
        replanner.replan(impact, workflowId, workflow.getCorrelationId());

        runToQuiescence(workflowId);
        return workflowRepository.findById(workflowId).orElseThrow();
    }

    @Transactional
    public void resume(UUID workflowId) {
        safeStoppedWorkflows.remove(workflowId);
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflowRepository.save(workflow);
        auditService.record(workflowId, null, AuditEventType.WORKFLOW_RESUMED, "human", AuditOutcome.SUCCESS,
                null, workflow.getCorrelationId());
        runToQuiescence(workflowId);
    }

    @Transactional
    public void finalizeIfComplete(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        if (workflow.getStatus() != WorkflowStatus.RUNNING) {
            return;
        }
        List<WorkflowNode> allNodes = workflowNodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
        boolean anyBlockedOrFailed = allNodes.stream()
                .anyMatch(n -> n.getStatus() == NodeStatus.FAILED || n.getStatus() == NodeStatus.BLOCKED);
        boolean allTerminal = allNodes.stream().allMatch(WorkflowNode::isTerminal);

        if (anyBlockedOrFailed && allNodes.stream().noneMatch(n -> n.getStatus() == NodeStatus.PENDING
                || n.getStatus() == NodeStatus.READY || n.getStatus() == NodeStatus.RUNNING
                || n.getStatus() == NodeStatus.RETRYING)) {
            workflow.setStatus(WorkflowStatus.FAILED);
            workflowRepository.save(workflow);
            metricsService.workflowFailed();
            auditService.record(workflowId, null, AuditEventType.WORKFLOW_FAILED, "orchestrator",
                    AuditOutcome.FAILURE, "One or more nodes failed/blocked", workflow.getCorrelationId());
            return;
        }

        if (allTerminal && !allNodes.isEmpty()) {
            workflow.setStatus(WorkflowStatus.SUCCEEDED);
            workflowRepository.save(workflow);
            metricsService.workflowSucceeded();
            metricsService.recordWorkflowDuration(Duration.between(workflow.getCreatedAt(), Instant.now()));
            auditService.record(workflowId, null, AuditEventType.WORKFLOW_COMPLETED, "orchestrator",
                    AuditOutcome.SUCCESS, null, workflow.getCorrelationId());
        }
    }
}
