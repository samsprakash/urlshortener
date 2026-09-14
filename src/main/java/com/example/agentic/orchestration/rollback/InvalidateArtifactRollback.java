package com.example.agentic.orchestration.rollback;

import com.example.agentic.artifact.Artifact;

/** Marks an artifact INVALIDATED rather than deleting it — audit history is preserved. */
public class InvalidateArtifactRollback implements RollbackAction {

    private final Artifact artifact;

    public InvalidateArtifactRollback(Artifact artifact) {
        this.artifact = artifact;
    }

    @Override
    public String description() {
        return "Invalidate artifact " + artifact.getType() + " v" + artifact.getVersion();
    }

    @Override
    public void compensate() {
        artifact.invalidate();
    }
}
