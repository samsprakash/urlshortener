CREATE TABLE audit_events (
    id                  UUID PRIMARY KEY,
    workflow_id         UUID REFERENCES workflows (id) ON DELETE CASCADE,
    node_id             UUID REFERENCES workflow_nodes (id) ON DELETE SET NULL,
    event_type          VARCHAR(64)  NOT NULL,
    actor               VARCHAR(64)  NOT NULL,
    outcome             VARCHAR(32)  NOT NULL,
    reason              TEXT,
    correlation_id      VARCHAR(64)  NOT NULL,
    artifact_versions   JSONB        NOT NULL DEFAULT '{}',
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_events_workflow_id ON audit_events (workflow_id);
CREATE INDEX idx_audit_events_correlation_id ON audit_events (correlation_id);
CREATE INDEX idx_audit_events_event_type ON audit_events (event_type);
