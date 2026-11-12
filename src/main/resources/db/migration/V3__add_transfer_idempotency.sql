CREATE TABLE IF NOT EXISTS transfer_idempotency (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    destination_account_id BIGINT NOT NULL REFERENCES ledger_accounts(id),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    transaction_id VARCHAR(36) NOT NULL,
    from_account_id BIGINT NOT NULL REFERENCES ledger_accounts(id),
    to_account_id BIGINT NOT NULL REFERENCES ledger_accounts(id),
    result_balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
