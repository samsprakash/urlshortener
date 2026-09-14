package com.example.agentic.orchestration.rollback;

import com.example.agentic.orchestration.state.WorkflowNode;

/** Resets a downstream node back to PENDING so it will be re-run after rollback + replan. */
public class ResetNodeRollback implements RollbackAction {

    private final WorkflowNode node;

    public ResetNodeRollback(WorkflowNode node) {
        this.node = node;
    }

    @Override
    public String description() {
        return "Reset node " + node.getNodeKey() + " to PENDING for re-execution";
    }

    @Override
    public void compensate() {
        node.resetForReplan();
    }
}
