package snovasys.ledger.transactions.model;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TransactionRequest {
    @NotNull @Positive @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;

    // Transfers specify a recipient; deposits/withdrawals use the signed-in holder's account.
    private Long destinationAccountId;

    // Required by transfer operations and checked against the holder's BCrypt MPIN hash.
    @Pattern(regexp = "\\d{6}", message = "MPIN must contain exactly 6 digits")
    private String mpin;
}
