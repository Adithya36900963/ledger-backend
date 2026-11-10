package snovasys.ledger.transactions.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.model.TransactionRequest;
import snovasys.ledger.transactions.service.TransactionResult;
import snovasys.ledger.transactions.service.WithdrawalService;
import snovasys.ledger.audit.service.AuditLogService;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/ledger/withdrawals") @RequiredArgsConstructor
public class WithdrawalController {
    private final WithdrawalService withdrawalService;
    private final AuditLogService auditLog;
    @PostMapping
    public TransactionResult withdraw(Authentication authentication, @Valid @RequestBody TransactionRequest request) {
        try { return withdrawalService.withdraw(authentication, request); }
        catch (ResponseStatusException exception) {
            auditLog.recordFailure(authentication, "WITHDRAWAL", exception.getReason());
            throw exception;
        }
    }
}
