package snovasys.ledger.audit.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_events", indexes = @Index(name = "idx_audit_events_created", columnList = "created_at"))
@Getter @Setter @NoArgsConstructor
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_id")
    private Long actorId;
    @Column(name = "actor_name", length = 160)
    private String actorName;
    @Column(name = "actor_role", length = 32)
    private String actorRole;
    @Column(nullable = false, length = 64)
    private String action;
    @Column(nullable = false, length = 24)
    private String outcome;
    @Column(nullable = false, length = 2000)
    private String details;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
