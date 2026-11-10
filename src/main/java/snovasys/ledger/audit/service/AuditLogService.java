package snovasys.ledger.audit.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.audit.entity.AuditEvent;
import snovasys.ledger.audit.model.AuditEventView;
import snovasys.ledger.audit.repository.AuditEventRepository;
import snovasys.ledger.users.entity.User;

@Service @RequiredArgsConstructor
public class AuditLogService {
    private final AuditEventRepository events;

    @Transactional
    public void record(Authentication authentication, String action, String outcome, String details) {
        events.save(toEvent(authentication, action, outcome, details));
    }

    @Transactional
    public void recordUser(User actor, String action, String outcome, String details) {
        events.save(toEvent(actor, action, outcome, details));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Authentication authentication, String action, String details) {
        events.save(toEvent(authentication, action, "FAILED", details));
    }

    @Transactional
    public void recordSystem(String action, String outcome, String details) {
        events.save(toEvent((User) null, action, outcome, details));
    }

    @Transactional(readOnly = true)
    public List<AuditEventView> all() {
        return events.findTop200ByOrderByCreatedAtDescIdDesc().stream().map(AuditLogService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventView> byActor(Long actorId) {
        return events.findTop200ByActorIdOrderByCreatedAtDescIdDesc(actorId).stream().map(AuditLogService::view).toList();
    }

    private AuditEvent toEvent(Authentication authentication, String action, String outcome, String details) {
        User actor = authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
        return toEvent(actor, action, outcome, details);
    }

    private AuditEvent toEvent(User actor, String action, String outcome, String details) {
        AuditEvent event = new AuditEvent();
        event.setActorId(actor == null ? null : actor.getId());
        event.setActorName(actor == null ? "system" : actor.getUsername());
        event.setActorRole(actor == null || actor.getRole() == null ? "SYSTEM" : actor.getRole().name());
        event.setAction(action);
        event.setOutcome(outcome);
        event.setDetails(details == null ? "" : details);
        event.setCreatedAt(Instant.now());
        return event;
    }

    private static AuditEventView view(AuditEvent event) {
        return new AuditEventView(event.getId(), event.getActorId(), event.getActorName(), event.getActorRole(),
                event.getAction(), event.getOutcome(), event.getDetails(), event.getCreatedAt());
    }
}
