package snovasys.ledger.transactions.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import snovasys.ledger.users.entity.ACCOUNT_HOLDER;

@Entity
@Table(name = "ledger_accounts")
@Getter @Setter @NoArgsConstructor
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_holder_id", unique = true)
    private ACCOUNT_HOLDER holder;

    @Column(name = "system_account", nullable = false)
    private boolean systemAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Version
    private long version;
}
