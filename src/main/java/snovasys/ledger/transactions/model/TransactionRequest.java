package snovasys.ledger.transactions.model;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TransactionRequest {
    @NotNull @Positive @DecimalMin(value = "0.01")
    private BigDecimal amount;

    // Transfers specify a recipient; deposits/withdrawals use the signed-in holder's account.
    private Long destinationAccountId;
}
