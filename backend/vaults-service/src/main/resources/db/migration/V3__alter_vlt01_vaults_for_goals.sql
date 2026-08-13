ALTER TABLE vlt01_vaults DROP CONSTRAINT vlt01_vaults_customer_id_key;

ALTER TABLE vlt01_vaults
    ADD COLUMN name VARCHAR(100),
    ADD COLUMN target_amount NUMERIC(18,4),
    ADD COLUMN sweep_amount NUMERIC(18,4),
    ADD COLUMN sweep_frequency VARCHAR(10) CHECK (sweep_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    ADD COLUMN sweep_active BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN next_sweep_at TIMESTAMPTZ,
    ADD COLUMN last_sweep_at TIMESTAMPTZ;
