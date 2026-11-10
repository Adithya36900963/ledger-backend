package snovasys.ledger.security.repo;


import org.springframework.data.jpa.repository.JpaRepository;

import snovasys.ledger.security.common.VerificationType;
import snovasys.ledger.security.entity.VerificationToken;

import java.util.Optional;


public interface VerificationTokenRepo
        extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    void deleteByUser_IdAndType(
            Long userId,
            VerificationType type
    );
}