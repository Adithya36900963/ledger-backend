package snovasys.ledger.transactions.model;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryView(Long id, String transactionId, Long accountId, String accountHolder,
        BigDecimal amount, String type, Instant createdAt) { }
