package snovasys.ledger.audit.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.audit.model.AuditEventView;
import snovasys.ledger.audit.service.AuditLogService;
import snovasys.ledger.transactions.model.LedgerEntryView;
import snovasys.ledger.transactions.model.LedgerIntegrityView;
import snovasys.ledger.transactions.service.LedgerReadService;
import snovasys.ledger.users.entity.User;

@RestController @RequestMapping("/api/audit") @RequiredArgsConstructor
public class AuditReadController {
    private final AuditLogService auditLogService;
    private final LedgerReadService ledgerReadService;

    @GetMapping("/events")
    public List<AuditEventView> events() { return auditLogService.all(); }

    @GetMapping("/ledger-entries")
    public List<LedgerEntryView> ledgerEntries() { return ledgerReadService.allEntries(); }

    @GetMapping("/integrity")
    public LedgerIntegrityView integrity() { return ledgerReadService.integrity(); }

    @GetMapping("/my-events")
    public List<AuditEventView> myEvents(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return auditLogService.byActor(user.getId());
    }
}
