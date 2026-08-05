CREATE TABLE ldg04_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_wallet_id UUID NOT NULL REFERENCES ldg02_wallets(id),
    to_wallet_id UUID NOT NULL REFERENCES ldg02_wallets(id),
    source_amount NUMERIC(18,4) NOT NULL,
    source_currency VARCHAR(3) NOT NULL CHECK (source_currency IN ('MYR', 'SGD', 'USD', 'EUR', 'GBP')),
    dest_amount NUMERIC(18,4) NOT NULL,
    dest_currency VARCHAR(3) NOT NULL CHECK (dest_currency IN ('MYR', 'SGD', 'USD', 'EUR', 'GBP')),
    fx_rate NUMERIC(18,8),
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
