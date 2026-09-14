CREATE TABLE workflows (
    id                      UUID PRIMARY KEY,
    scenario                VARCHAR(32)  NOT NULL,
    status                  VARCHAR(32)  NOT NULL,
    requirement_text        TEXT         NOT NULL,
    requirement_version     INT          NOT NULL DEFAULT 1,
    correlation_id          VARCHAR(64)  NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now(),
    completed_at            TIMESTAMP
);

CREATE INDEX idx_workflows_status ON workflows (status);
CREATE INDEX idx_workflows_correlation_id ON workflows (correlation_id);

CREATE TABLE workflow_nodes (
    id              UUID PRIMARY KEY,
    workflow_id     UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    node_key        VARCHAR(64)  NOT NULL,
    agent_type      VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    risk_level      VARCHAR(16)  NOT NULL DEFAULT 'LOW',
    retry_count     INT          NOT NULL DEFAULT 0,
    max_retries     INT          NOT NULL DEFAULT 2,
    depends_on      JSONB        NOT NULL DEFAULT '[]',
    error_message   TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (workflow_id, node_key)
);

CREATE INDEX idx_workflow_nodes_workflow_id ON workflow_nodes (workflow_id);
CREATE INDEX idx_workflow_nodes_status ON workflow_nodes (status);
