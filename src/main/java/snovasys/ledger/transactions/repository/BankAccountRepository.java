package snovasys.ledger.transactions.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import snovasys.ledger.transactions.entity.BankAccount;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByHolder_Id(Long holderId);
    Optional<BankAccount> findBySystemAccountTrue();
    java.util.List<BankAccount> findBySystemAccountFalseOrderByIdDesc();
    long countBySystemAccountFalse();
    long countBySystemAccountFalseAndBalanceLessThan(java.math.BigDecimal balance);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
