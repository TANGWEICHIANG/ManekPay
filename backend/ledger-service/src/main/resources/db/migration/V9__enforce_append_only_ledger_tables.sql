-- Application code never issues UPDATE/DELETE against these three transaction-recording tables
-- (ldg04_transfers, ldg05_ledger_entries, ldg06_vault_sweep_debits) - each row is a permanent
-- record of money that already moved. That has only ever been enforced by discipline in the
-- application layer; nothing in the database itself stopped a future bug, a bad migration, or
-- direct DB access from silently corrupting the audit trail. This closes that gap at the source
-- of truth, independent of which application code path (if any) touches the table.
CREATE OR REPLACE FUNCTION reject_ledger_table_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'append-only table %: % is not permitted', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ldg04_transfers_append_only
    BEFORE UPDATE OR DELETE ON ldg04_transfers
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_table_mutation();

CREATE TRIGGER ldg05_ledger_entries_append_only
    BEFORE UPDATE OR DELETE ON ldg05_ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_table_mutation();

CREATE TRIGGER ldg06_vault_sweep_debits_append_only
    BEFORE UPDATE OR DELETE ON ldg06_vault_sweep_debits
    FOR EACH ROW EXECUTE FUNCTION reject_ledger_table_mutation();
