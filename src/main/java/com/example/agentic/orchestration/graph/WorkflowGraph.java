package com.example.agentic.orchestration.graph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An ordered, acyclic set of node specs. Validated at construction time so a
 * malformed graph (cycle, dangling dependency) fails fast instead of hanging
 * the scheduler at runtime.
 */
public final class WorkflowGraph {

    private final Map<String, GraphNodeSpec> nodesByKey;

    private WorkflowGraph(Map<String, GraphNodeSpec> nodesByKey) {
        this.nodesByKey = nodesByKey;
    }

    public static WorkflowGraph of(List<GraphNodeSpec> specs) {
        Map<String, GraphNodeSpec> byKey = new LinkedHashMap<>();
        for (GraphNodeSpec spec : specs) {
            if (byKey.containsKey(spec.nodeKey())) {
                throw new IllegalArgumentException("Duplicate node key in graph: " + spec.nodeKey());
            }
            byKey.put(spec.nodeKey(), spec);
        }
        for (GraphNodeSpec spec : specs) {
            for (String dep : spec.dependsOn()) {
                if (!byKey.containsKey(dep)) {
                    throw new IllegalArgumentException(
                            "Node '" + spec.nodeKey() + "' depends on unknown node '" + dep + "'");
                }
            }
        }
        DependencyResolver.assertAcyclic(byKey);
        return new WorkflowGraph(byKey);
    }

    public List<GraphNodeSpec> nodes() {
        return List.copyOf(nodesByKey.values());
    }

    public GraphNodeSpec get(String nodeKey) {
        GraphNodeSpec spec = nodesByKey.get(nodeKey);
        if (spec == null) {
            throw new IllegalArgumentException("No such node: " + nodeKey);
        }
        return spec;
    }
}
