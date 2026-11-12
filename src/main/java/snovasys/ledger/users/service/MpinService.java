package snovasys.ledger.users.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.repository.UserRepo;
import snovasys.ledger.security.rateLimiting.MpinAttemptLimiter;

@Service
@RequiredArgsConstructor
public class MpinService {
    private final UserRepo users;
    private final PasswordEncoder passwordEncoder;
    private final MpinAttemptLimiter attemptLimiter;

    public boolean isConfigured(Authentication authentication) {
        User user = accountHolder(authentication);
        return user.getMpinHash() != null;
    }

    @Transactional
    public void setup(Authentication authentication, String mpin) {
        User user = accountHolder(authentication);
        validateFormat(mpin);
        if (user.getMpinHash() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MPIN is already configured");
        }
        user.setMpinHash(passwordEncoder.encode(mpin));
        users.save(user);
    }

    public void verify(User user, String mpin) {
        validateFormat(mpin);
        if (user.getMpinHash() == null) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Set up your MPIN before transferring");
        }
        long blockedFor = attemptLimiter.blockedForSeconds(user.getId());
        if (blockedFor > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect MPIN attempts. Try again in " + blockedFor + " seconds");
        }
        if (!passwordEncoder.matches(mpin, user.getMpinHash())) {
            attemptLimiter.recordFailure(user.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect MPIN");
        }
        attemptLimiter.recordSuccess(user.getId());
    }

    private User accountHolder(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (user.getRole() != Role.ACCOUNT_HOLDER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MPIN is available to account holders only");
        }
        return user;
    }

    private void validateFormat(String mpin) {
        if (mpin == null || !mpin.matches("\\d{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MPIN must contain exactly 6 digits");
        }
    }
}
