package com.example.agentic.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentRegistry {

    private final Map<AgentType, Agent> agentsByType;

    public AgentRegistry(List<Agent> agents) {
        this.agentsByType = agents.stream().collect(Collectors.toMap(Agent::type, Function.identity()));
    }

    public Agent get(AgentType type) {
        Agent agent = agentsByType.get(type);
        if (agent == null) {
            throw new IllegalStateException("No agent registered for type " + type);
        }
        return agent;
    }
}
