package snovasys.ledger.transactions.model;

import java.math.BigDecimal;

public record LedgerIntegrityView(boolean balanced, BigDecimal ledgerTotal,
        long customerAccountCount, long negativeBalanceCount) { }
