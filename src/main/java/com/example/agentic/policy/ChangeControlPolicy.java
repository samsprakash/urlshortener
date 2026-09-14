package com.example.agentic.policy;

import com.example.agentic.agent.AgentAction;
import com.example.agentic.agent.AgentContext;
import com.example.agentic.agent.AgentType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Risk -> autonomy mapping from 01-architecture.md §4:
 *   LOW      -> agent executes autonomously
 *   MEDIUM   -> agent executes, audited
 *   HIGH     -> human approval required
 *   CRITICAL -> human approval + security review
 *
 * Schema/architecture changes on a brownfield system are always HIGH regardless
 * of the node's declared risk, because they can break existing clients.
 */
@Component
@Order(10)
public class ChangeControlPolicy implements Policy {

    @Override
    public PolicyDecision evaluate(AgentAction action, AgentContext context) {
        if (action.agentType() == AgentType.ARCHITECTURE && "schema".equals(action.nodeKey())) {
            return PolicyDecision.requireApproval("Database schema changes require human approval (HIGH risk)");
        }
        return switch (action.declaredRisk()) {
            case LOW, MEDIUM -> PolicyDecision.allow("Risk level " + action.declaredRisk() + " permits autonomous execution");
            case HIGH -> PolicyDecision.requireApproval("HIGH risk action requires human approval");
            case CRITICAL -> PolicyDecision.requireApproval("CRITICAL risk action requires human approval and security review");
        };
    }
}
