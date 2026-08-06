CREATE TABLE vlt01_vaults (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('MYR', 'SGD', 'USD', 'EUR', 'GBP')),
    balance NUMERIC(18,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
