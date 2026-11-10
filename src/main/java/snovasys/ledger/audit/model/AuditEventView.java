package snovasys.ledger.audit.model;

import java.time.Instant;

public record AuditEventView(Long id, Long actorId, String actorName, String actorRole,
        String action, String outcome, String details, Instant createdAt) { }
