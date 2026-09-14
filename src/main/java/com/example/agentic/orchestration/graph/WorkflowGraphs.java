package com.example.agentic.orchestration.graph;

import com.example.agentic.agent.AgentType;
import com.example.agentic.policy.RiskLevel;

import java.util.List;

/**
 * Base graph shapes for each scenario, per 01-architecture.md §5 and 03-scenarios.md.
 * These are blueprints (GraphNodeSpec), not persisted state — WorkflowEngine
 * materializes the concrete WorkflowNode rows from whichever graph a scenario
 * selects.
 */
public final class WorkflowGraphs {

    private WorkflowGraphs() {
    }

    /** requirements -> (architecture || planning) -> implementation -> (testing || security) -> documentation -> release -> approval */
    public static WorkflowGraph greenfield() {
        return WorkflowGraph.of(List.of(
                GraphNodeSpec.of("requirements", AgentType.REQUIREMENTS, RiskLevel.LOW),
                GraphNodeSpec.of("architecture", AgentType.ARCHITECTURE, RiskLevel.LOW, "requirements"),
                GraphNodeSpec.of("planning", AgentType.PLANNING, RiskLevel.LOW, "requirements"),
                GraphNodeSpec.of("implementation", AgentType.DEVELOPER, RiskLevel.MEDIUM, "architecture", "planning"),
                GraphNodeSpec.of("testing", AgentType.TEST, RiskLevel.MEDIUM, "implementation"),
                GraphNodeSpec.of("security", AgentType.SECURITY, RiskLevel.MEDIUM, "implementation"),
                GraphNodeSpec.of("documentation", AgentType.DOCUMENTATION, RiskLevel.LOW, "testing", "security"),
                GraphNodeSpec.of("release", AgentType.RELEASE, RiskLevel.HIGH, "documentation")
        ));
    }

    /** requirement -> impact analysis -> (schema || implementation || test-plan) -> regression -> security -> approval -> release */
    public static WorkflowGraph brownfield() {
        return WorkflowGraph.of(List.of(
                GraphNodeSpec.of("requirements", AgentType.REQUIREMENTS, RiskLevel.LOW),
                GraphNodeSpec.of("impact_analysis", AgentType.IMPACT_ANALYSIS, RiskLevel.LOW, "requirements"),
                GraphNodeSpec.of("schema", AgentType.ARCHITECTURE, RiskLevel.HIGH, "impact_analysis"),
                GraphNodeSpec.of("implementation", AgentType.DEVELOPER, RiskLevel.MEDIUM, "impact_analysis"),
                GraphNodeSpec.of("test_plan", AgentType.PLANNING, RiskLevel.LOW, "impact_analysis"),
                GraphNodeSpec.of("regression_testing", AgentType.TEST, RiskLevel.MEDIUM, "schema", "implementation", "test_plan"),
                GraphNodeSpec.of("security", AgentType.SECURITY, RiskLevel.MEDIUM, "regression_testing"),
                GraphNodeSpec.of("release", AgentType.RELEASE, RiskLevel.HIGH, "security")
        ));
    }

    /** requirement -> ambiguity detection (parks at CLARIFICATION_REQUIRED if ambiguous) */
    public static WorkflowGraph ambiguous() {
        return WorkflowGraph.of(List.of(
                GraphNodeSpec.of("requirements", AgentType.REQUIREMENTS, RiskLevel.LOW)
        ));
    }

    /** Once clarified, the ambiguous scenario continues through the greenfield shape rooted past requirements. */
    public static WorkflowGraph ambiguousResolved() {
        return greenfield();
    }
}
