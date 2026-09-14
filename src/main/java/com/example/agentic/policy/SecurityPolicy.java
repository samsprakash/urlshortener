package com.example.agentic.policy;

import com.example.agentic.agent.AgentAction;
import com.example.agentic.agent.AgentContext;
import com.example.agentic.agent.AgentType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Denies actions a SecurityAgent has flagged as failing, and escalates release
 * to CRITICAL + approval when the workflow is releasing a security-sensitive
 * change (destructive/production action per 01-architecture.md §4).
 */
@Component
@Order(20)
public class SecurityPolicy implements Policy {

    @Override
    public PolicyDecision evaluate(AgentAction action, AgentContext context) {
        Object securityFailed = context.metadataValue("securityCheckFailed");
        if (Boolean.TRUE.equals(securityFailed) && action.agentType() != AgentType.SECURITY) {
            return PolicyDecision.deny("A prior security check failed; downstream actions are denied until resolved");
        }
        if (action.agentType() == AgentType.RELEASE) {
            return PolicyDecision.requireApproval("Production release is a CRITICAL, destructive-capable action");
        }
        return PolicyDecision.allow("No security concerns raised");
    }
}
