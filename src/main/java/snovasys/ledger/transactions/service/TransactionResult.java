package snovasys.ledger.transactions.service;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResult(String transactionId, String type, Long fromAccountId,
        Long toAccountId, BigDecimal amount, BigDecimal balance, Instant createdAt) { }
