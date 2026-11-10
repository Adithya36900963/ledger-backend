package snovasys.ledger.transactions.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.model.TransactionRequest;
import snovasys.ledger.transactions.service.TransactionResult;
import snovasys.ledger.transactions.service.TransferService;
import snovasys.ledger.audit.service.AuditLogService;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/ledger/transfers") @RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;
    private final AuditLogService auditLog;
    @PostMapping
    public TransactionResult transfer(Authentication authentication, @Valid @RequestBody TransactionRequest request) {
        try { return transferService.transfer(authentication, request); }
        catch (ResponseStatusException exception) {
            auditLog.recordFailure(authentication, "TRANSFER", exception.getReason());
            throw exception;
        }
    }
}
