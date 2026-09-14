CREATE TABLE artifacts (
    id                      UUID PRIMARY KEY,
    workflow_id             UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    node_id                 UUID REFERENCES workflow_nodes (id) ON DELETE SET NULL,
    type                    VARCHAR(64)  NOT NULL,
    version                 INT          NOT NULL DEFAULT 1,
    content                 JSONB        NOT NULL,
    source_artifact_ids     JSONB        NOT NULL DEFAULT '[]',
    status                  VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_by              VARCHAR(64)  NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_artifacts_workflow_id ON artifacts (workflow_id);
CREATE INDEX idx_artifacts_type ON artifacts (workflow_id, type);

CREATE TABLE decisions (
    id              UUID PRIMARY KEY,
    workflow_id     UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    artifact_id     UUID REFERENCES artifacts (id) ON DELETE SET NULL,
    decision        VARCHAR(256) NOT NULL,
    rationale       TEXT         NOT NULL,
    alternatives    JSONB        NOT NULL DEFAULT '[]',
    selected        VARCHAR(256) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_decisions_workflow_id ON decisions (workflow_id);
