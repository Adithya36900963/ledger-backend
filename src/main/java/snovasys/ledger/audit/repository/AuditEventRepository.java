package snovasys.ledger.audit.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snovasys.ledger.audit.entity.AuditEvent;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop200ByOrderByCreatedAtDescIdDesc();
    List<AuditEvent> findTop200ByActorIdOrderByCreatedAtDescIdDesc(Long actorId);
}
