-- V9: Backfill legacy transaction timestamps so balanceAfterTransaction can be computed for old rows

-- Ensure created_at exists for legacy transactions.
UPDATE transactions
SET created_at = COALESCE(transaction_date, updated_at, NOW())
WHERE created_at IS NULL;

-- Ensure transaction_date exists for legacy transactions.
UPDATE transactions
SET transaction_date = COALESCE(transaction_date, created_at, updated_at, NOW())
WHERE transaction_date IS NULL;

-- Keep updated_at consistent for rows where it was missing.
UPDATE transactions
SET updated_at = COALESCE(updated_at, created_at, transaction_date, NOW())
WHERE updated_at IS NULL;
