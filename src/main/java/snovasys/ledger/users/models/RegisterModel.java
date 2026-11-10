package snovasys.ledger.users.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterModel {
    @NotBlank @Size(max = 120)
    private String userName;
    @NotBlank @Email @Size(max = 254)
    private String email;
    @NotBlank @Size(min = 6, max = 10)
    private String number;
    @NotNull
    private Role role;
    @NotBlank @Size(min = 12, max = 100)
    private String password;
    private String businessName;

    
}
