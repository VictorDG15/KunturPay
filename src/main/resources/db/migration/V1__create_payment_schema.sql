CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    merchant_id UUID NOT NULL,
    order_id VARCHAR(80) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('PEN', 'USD')),
    payment_token VARCHAR(120) NOT NULL,
    description VARCHAR(200),
    status VARCHAR(30) NOT NULL,
    authorization_code VARCHAR(80),
    decline_reason VARCHAR(160),
    refunded_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payments_merchant_order UNIQUE (merchant_id, order_id)
);

CREATE INDEX idx_payments_merchant_created
    ON payments (merchant_id, created_at DESC);

CREATE INDEX idx_payments_status_updated
    ON payments (status, updated_at);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);

CREATE INDEX idx_outbox_pending
    ON outbox_events (next_attempt_at, occurred_at)
    WHERE published_at IS NULL;
