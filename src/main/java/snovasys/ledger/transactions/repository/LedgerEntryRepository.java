package snovasys.ledger.transactions.repository;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snovasys.ledger.transactions.entity.LedgerEntry;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccount_IdOrderByCreatedAtDescIdDesc(Long accountId);
    List<LedgerEntry> findTop200ByOrderByCreatedAtDescIdDesc();

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e")
    BigDecimal sumAllEntries();
}
