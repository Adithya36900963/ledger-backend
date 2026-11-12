package snovasys.ledger.transactions.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferHistoryView(String transactionId, Long fromAccountId, Long toAccountId,
        Long counterpartyAccountId, String counterpartyName, BigDecimal amount, Instant createdAt) { }
