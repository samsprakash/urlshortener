CREATE TABLE urls (
    id              UUID PRIMARY KEY,
    short_code      VARCHAR(16)  NOT NULL UNIQUE,
    original_url    VARCHAR(2048) NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP,
    click_count     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_urls_short_code ON urls (short_code);
CREATE INDEX idx_urls_status ON urls (status);
