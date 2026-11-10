package snovasys.ledger.transactions.entity;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_entry_account_time", columnList = "account_id, created_at"),
        @Index(name = "idx_ledger_entry_transaction", columnList = "transaction_id")
})
@Getter @Setter @NoArgsConstructor
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 36)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    // Positive is a credit; negative is a debit.
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
