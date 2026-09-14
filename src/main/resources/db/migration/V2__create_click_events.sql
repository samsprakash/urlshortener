CREATE TABLE click_events (
    id              UUID PRIMARY KEY,
    url_id          UUID NOT NULL REFERENCES urls (id) ON DELETE CASCADE,
    occurred_at     TIMESTAMP NOT NULL DEFAULT now(),
    visitor_hash    VARCHAR(64) NOT NULL,
    user_agent      VARCHAR(512),
    referrer        VARCHAR(1024)
);

CREATE INDEX idx_click_events_url_id ON click_events (url_id);
CREATE INDEX idx_click_events_occurred_at ON click_events (occurred_at);
