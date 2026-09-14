package com.example.agentic.orchestration.rollback;

/**
 * A compensating action, not a transactional rollback (ADR-7): agent actions can
 * span external systems (files, git, test runners) that aren't inside a single
 * DB transaction, so "rollback" means "restore a known-good state."
 */
public interface RollbackAction {

    String description();

    void compensate();
}
