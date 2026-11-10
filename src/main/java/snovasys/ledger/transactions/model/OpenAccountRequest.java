package snovasys.ledger.transactions.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OpenAccountRequest {
    @NotNull @Positive
    private Long accountHolderId;
}
