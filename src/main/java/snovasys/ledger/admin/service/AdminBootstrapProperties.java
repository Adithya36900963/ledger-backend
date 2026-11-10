package snovasys.ledger.admin.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.bootstrap.admin")
@Getter @Setter
public class AdminBootstrapProperties {
    private boolean enabled;
    private String username;
    private String email;
    private String number;
    private String password;
}
