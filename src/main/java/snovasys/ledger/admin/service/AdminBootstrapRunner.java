package snovasys.ledger.admin.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.audit.service.AuditLogService;
import snovasys.ledger.users.entity.Admin;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.repository.AdminRepo;
import snovasys.ledger.users.repository.UserRepo;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {
    private final AdminBootstrapProperties properties;
    private final UserRepo users;
    private final AdminRepo admins;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLog;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) return;
        if (users.existsByRole(Role.ADMIN)) return;
        requireConfiguration();
        if (users.findByEmailOrNumber(properties.getEmail()) != null
                || users.findByEmailOrNumber(properties.getNumber()) != null) {
            throw new IllegalStateException("Bootstrap admin email or phone already belongs to a non-admin user");
        }

        User user = users.save(User.builder()
                .username(properties.getUsername().trim())
                .email(properties.getEmail().trim().toLowerCase())
                .number(properties.getNumber().trim())
                .password(passwordEncoder.encode(properties.getPassword()))
                .role(Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .build());
        Admin admin = new Admin();
        admin.setUser(user);
        admins.save(admin);
        auditLog.recordSystem("ADMIN_BOOTSTRAP", "SUCCESS", "Initial administrator provisioned: userId=" + user.getId());
    }

    private void requireConfiguration() {
        if (!StringUtils.hasText(properties.getUsername()) || !StringUtils.hasText(properties.getEmail())
                || !StringUtils.hasText(properties.getNumber()) || !StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException("When BOOTSTRAP_ADMIN_ENABLED is true, BOOTSTRAP_ADMIN_USERNAME, "
                    + "BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_NUMBER, and BOOTSTRAP_ADMIN_PASSWORD are required");
        }
        if (properties.getNumber().trim().length() > 10)
            throw new IllegalStateException("BOOTSTRAP_ADMIN_NUMBER must be 10 characters or fewer");
        if (properties.getPassword().length() < 12)
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
    }
}
