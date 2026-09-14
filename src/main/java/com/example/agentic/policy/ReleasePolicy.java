package com.example.agentic.policy;

import com.example.agentic.agent.AgentAction;
import com.example.agentic.agent.AgentContext;
import com.example.agentic.agent.AgentType;
import com.example.agentic.artifact.Artifact;
import com.example.agentic.artifact.ArtifactType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A release cannot proceed without a passing TestReport and SecurityReport
 * artifact already in context — this is the policy-level guarantee behind
 * "release review" in the workflow graph.
 */
@Component
@Order(30)
public class ReleasePolicy implements Policy {

    @Override
    public PolicyDecision evaluate(AgentAction action, AgentContext context) {
        if (action.agentType() != AgentType.RELEASE) {
            return PolicyDecision.allow("Not a release action");
        }
        Artifact testReport = context.artifact(ArtifactType.TEST_REPORT);
        Artifact securityReport = context.artifact(ArtifactType.SECURITY_REPORT);
        if (testReport == null) {
            return PolicyDecision.deny("Cannot release without a TestReport artifact");
        }
        if (securityReport == null) {
            return PolicyDecision.deny("Cannot release without a SecurityReport artifact");
        }
        Object testPassed = testReport.getContent().get("passed");
        if (Boolean.FALSE.equals(testPassed)) {
            return PolicyDecision.deny("Cannot release: latest TestReport indicates failure");
        }
        Object securityPassed = securityReport.getContent().get("passed");
        if (Boolean.FALSE.equals(securityPassed)) {
            return PolicyDecision.deny("Cannot release: latest SecurityReport indicates failure");
        }
        return PolicyDecision.requireApproval("Release checks passed; human approval required before going live");
    }
}
