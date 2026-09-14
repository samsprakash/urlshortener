package com.example.agentic.policy;

import com.example.agentic.agent.AgentAction;
import com.example.agentic.agent.AgentContext;
import com.example.agentic.agent.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEngineTest {

    private final AgentContext emptyContext = new AgentContext(UUID.randomUUID(), "n", "req", Map.of(), Map.of());

    @Test
    void denyTakesPrecedenceOverAllowAndApproval() {
        Policy allow = (a, c) -> PolicyDecision.allow("ok");
        Policy approve = (a, c) -> PolicyDecision.requireApproval("needs approval");
        Policy deny = (a, c) -> PolicyDecision.deny("blocked");
        PolicyEngine engine = new PolicyEngine(List.of(allow, approve, deny));

        AgentAction action = new AgentAction(AgentType.RELEASE, "release", RiskLevel.HIGH, "desc");
        PolicyDecision result = engine.evaluate(action, emptyContext);

        assertThat(result.type()).isEqualTo(DecisionType.DENY);
    }

    @Test
    void requireApprovalTakesPrecedenceOverAllow() {
        Policy allow = (a, c) -> PolicyDecision.allow("ok");
        Policy approve = (a, c) -> PolicyDecision.requireApproval("needs approval");
        PolicyEngine engine = new PolicyEngine(List.of(allow, approve));

        AgentAction action = new AgentAction(AgentType.RELEASE, "release", RiskLevel.HIGH, "desc");
        PolicyDecision result = engine.evaluate(action, emptyContext);

        assertThat(result.type()).isEqualTo(DecisionType.REQUIRE_APPROVAL);
    }

    @Test
    void allowsWhenNoPolicyObjects() {
        PolicyEngine engine = new PolicyEngine(List.of());
        AgentAction action = new AgentAction(AgentType.REQUIREMENTS, "requirements", RiskLevel.LOW, "desc");
        PolicyDecision result = engine.evaluate(action, emptyContext);
        assertThat(result.type()).isEqualTo(DecisionType.ALLOW);
    }

    @Test
    void changeControlPolicyRequiresApprovalForHighRisk() {
        ChangeControlPolicy policy = new ChangeControlPolicy();
        AgentAction action = new AgentAction(AgentType.RELEASE, "release", RiskLevel.HIGH, "desc");
        PolicyDecision result = policy.evaluate(action, emptyContext);
        assertThat(result.type()).isEqualTo(DecisionType.REQUIRE_APPROVAL);
    }

    @Test
    void changeControlPolicyAllowsLowRisk() {
        ChangeControlPolicy policy = new ChangeControlPolicy();
        AgentAction action = new AgentAction(AgentType.REQUIREMENTS, "requirements", RiskLevel.LOW, "desc");
        PolicyDecision result = policy.evaluate(action, emptyContext);
        assertThat(result.type()).isEqualTo(DecisionType.ALLOW);
    }

    @Test
    void changeControlPolicyAlwaysEscalatesSchemaNodeRegardlessOfDeclaredRisk() {
        ChangeControlPolicy policy = new ChangeControlPolicy();
        AgentAction action = new AgentAction(AgentType.ARCHITECTURE, "schema", RiskLevel.LOW, "desc");
        PolicyDecision result = policy.evaluate(action, emptyContext);
        assertThat(result.type()).isEqualTo(DecisionType.REQUIRE_APPROVAL);
    }

    @Test
    void securityPolicyDeniesDownstreamAfterSecurityCheckFailed() {
        SecurityPolicy policy = new SecurityPolicy();
        AgentContext context = new AgentContext(UUID.randomUUID(), "n", "req", Map.of(),
                Map.of("securityCheckFailed", true));
        AgentAction action = new AgentAction(AgentType.DOCUMENTATION, "documentation", RiskLevel.LOW, "desc");
        PolicyDecision result = policy.evaluate(action, context);
        assertThat(result.type()).isEqualTo(DecisionType.DENY);
    }
}
