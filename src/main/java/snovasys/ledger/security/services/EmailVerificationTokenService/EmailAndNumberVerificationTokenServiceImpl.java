package snovasys.ledger.security.services.EmailVerificationTokenService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import snovasys.ledger.security.common.VerificationType;
import snovasys.ledger.security.entity.VerificationToken;
import snovasys.ledger.security.repo.VerificationTokenRepo;
import snovasys.ledger.users.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailAndNumberVerificationTokenServiceImpl
        implements EmailAndNumberVerificationTokenService {

    private final VerificationTokenRepo tokenRepo;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    @Transactional 
    @Override
    public VerificationToken createEmailToken(User user) {

        tokenRepo.deleteByUser_IdAndType(
                user.getId(),
                VerificationType.EMAIL
        );

        String token = generateToken();

        VerificationToken verificationToken =
                VerificationToken.builder()
                        .token(token)
                        .user(user)
                        .type(VerificationType.EMAIL)
                        .expiresAt(
                                LocalDateTime.now().plusHours(24)
                        )
                        .used(false)
                        .build();

        return tokenRepo.save(verificationToken);
    }

    @Transactional 
    @Override
    public VerificationToken createMobileToken(User user) {

        tokenRepo.deleteByUser_IdAndType(
                user.getId(),
                VerificationType.MOBILE
        );

        String token = generateToken();

        VerificationToken verificationToken =
                VerificationToken.builder()
                        .token(token)
                        .user(user)
                        .type(VerificationType.MOBILE)
                        .expiresAt(
                                LocalDateTime.now().plusMinutes(10)
                        )
                        .used(false)
                        .build();

        return tokenRepo.save(verificationToken);
    }

    @Override
    public User verifyEmailToken(String token) {

        VerificationToken verificationToken =
                getValidToken(token);

        if (verificationToken.getType()
                != VerificationType.EMAIL) {

            throw new RuntimeException(
                    "Invalid email verification token"
            );
        }

        verificationToken.setUsed(true);
        tokenRepo.save(verificationToken);

        User user = verificationToken.getUser();

        user.setEmailVerified(true);

        return user;
    }

    @Override
    public User verifyMobileToken(String token) {

        VerificationToken verificationToken =
                getValidToken(token);

        if (verificationToken.getType()
                != VerificationType.MOBILE) {

            throw new RuntimeException(
                    "Invalid mobile verification token"
            );
        }

        verificationToken.setUsed(true);
        tokenRepo.save(verificationToken);

        User user = verificationToken.getUser();

        user.setMobileVerified(true);

        return user;
    }

    private VerificationToken getValidToken(String token) {

        VerificationToken verificationToken =
                tokenRepo.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid verification token"
                                )
                        );

        if (verificationToken.isUsed()) {
            throw new RuntimeException(
                    "Verification token already used"
            );
        }

        if (verificationToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Verification token expired"
            );
        }

        return verificationToken;
    }
}