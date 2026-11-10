package snovasys.ledger.security.services.EmailVerificationTokenService;

import snovasys.ledger.security.entity.VerificationToken;
import snovasys.ledger.users.entity.User;

public interface EmailAndNumberVerificationTokenService {

    String generateToken();

    VerificationToken createEmailToken(User user);

    VerificationToken createMobileToken(User user);

    User verifyEmailToken(String token);

    User verifyMobileToken(String token);
}