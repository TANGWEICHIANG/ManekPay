CREATE TABLE ldg03_account_proxies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES ldg01_accounts(id),
    type VARCHAR(10) NOT NULL CHECK (type IN ('NRIC', 'MOBILE')),
    value VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (type, value)
);
