package snovasys.ledger.transactions.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.model.TransactionRequest;
import snovasys.ledger.transactions.service.DepositService;
import snovasys.ledger.transactions.service.TransactionResult;
import snovasys.ledger.audit.service.AuditLogService;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/ledger/deposits") @RequiredArgsConstructor
public class DepositController {
    private final DepositService depositService;
    private final AuditLogService auditLog;
    @PostMapping
    public TransactionResult deposit(Authentication authentication, @Valid @RequestBody TransactionRequest request) {
        try { return depositService.deposit(authentication, request); }
        catch (ResponseStatusException exception) {
            auditLog.recordFailure(authentication, "DEPOSIT", exception.getReason());
            throw exception;
        }
    }
}
