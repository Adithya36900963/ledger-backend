package snovasys.ledger.users.models;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MpinRequest {
    @Pattern(regexp = "\\d{6}", message = "MPIN must contain exactly 6 digits")
    private String mpin;
}
