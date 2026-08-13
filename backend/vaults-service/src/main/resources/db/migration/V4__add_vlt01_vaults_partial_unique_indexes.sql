-- Preserves "exactly one default (round-up) vault per customer" without constraining goal vaults.
CREATE UNIQUE INDEX ux_vlt01_vaults_default ON vlt01_vaults(customer_id) WHERE name IS NULL;

-- No duplicate goal names per customer.
CREATE UNIQUE INDEX ux_vlt01_vaults_goal_name ON vlt01_vaults(customer_id, name) WHERE name IS NOT NULL;
