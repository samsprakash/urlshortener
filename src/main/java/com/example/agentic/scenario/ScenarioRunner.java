package com.example.agentic.scenario;

import com.example.agentic.orchestration.engine.WorkflowEngine;
import com.example.agentic.orchestration.graph.WorkflowGraph;
import com.example.agentic.orchestration.graph.WorkflowGraphs;
import com.example.agentic.orchestration.state.Scenario;
import com.example.agentic.orchestration.state.Workflow;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Maps a scenario request to the base graph shape it should run, per
 * 03-scenarios.md. This is deliberately thin: WorkflowGraphs owns the shapes,
 * WorkflowEngine owns execution; this class just selects.
 */
@Service
public class ScenarioRunner {

    private final WorkflowEngine workflowEngine;

    public ScenarioRunner(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    public Workflow run(Scenario scenario, String requirementText, Map<String, Object> metadata) {
        WorkflowGraph graph = switch (scenario) {
            case GREENFIELD -> WorkflowGraphs.greenfield();
            case BROWNFIELD -> WorkflowGraphs.brownfield();
            case AMBIGUOUS -> WorkflowGraphs.ambiguous();
        };
        return workflowEngine.start(scenario, requirementText, graph, metadata);
    }
}
