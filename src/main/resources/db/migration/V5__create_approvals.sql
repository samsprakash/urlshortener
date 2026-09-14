CREATE TABLE approvals (
    id              UUID PRIMARY KEY,
    workflow_id     UUID NOT NULL REFERENCES workflows (id) ON DELETE CASCADE,
    node_id         UUID NOT NULL REFERENCES workflow_nodes (id) ON DELETE CASCADE,
    action          VARCHAR(256) NOT NULL,
    risk_level      VARCHAR(16)  NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    approver        VARCHAR(128),
    comment         TEXT,
    requested_at    TIMESTAMP    NOT NULL DEFAULT now(),
    decided_at      TIMESTAMP
);

CREATE INDEX idx_approvals_workflow_id ON approvals (workflow_id);
CREATE INDEX idx_approvals_status ON approvals (status);
