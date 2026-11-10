package snovasys.ledger.audit.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.audit.model.AuditEventView;
import snovasys.ledger.audit.service.AuditLogService;
import snovasys.ledger.users.entity.User;

@RestController @RequestMapping("/api/ledger/audit-events") @RequiredArgsConstructor
public class MyAuditController {
    private final AuditLogService auditLogService;

    @GetMapping("/me")
    public List<AuditEventView> myEvents(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return auditLogService.byActor(user.getId());
    }
}
