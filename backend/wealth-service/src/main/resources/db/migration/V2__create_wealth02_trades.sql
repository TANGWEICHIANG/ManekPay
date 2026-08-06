CREATE TABLE wealth02_trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    asset_id UUID NOT NULL REFERENCES wealth01_assets(id),
    amount NUMERIC(18,4) NOT NULL,
    shares NUMERIC(18,4) NOT NULL,
    price_per_share NUMERIC(18,4) NOT NULL,
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
