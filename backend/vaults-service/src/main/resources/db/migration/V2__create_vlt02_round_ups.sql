CREATE TABLE vlt02_round_ups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vault_id UUID NOT NULL REFERENCES vlt01_vaults(id),
    transaction_id UUID NOT NULL UNIQUE,
    amount NUMERIC(18,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
