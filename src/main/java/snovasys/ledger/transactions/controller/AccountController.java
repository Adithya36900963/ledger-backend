package snovasys.ledger.transactions.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.model.OpenAccountRequest;
import snovasys.ledger.transactions.service.AccountService;
import snovasys.ledger.transactions.service.AccountService.AccountSummary;
import snovasys.ledger.transactions.service.AccountService.RecipientSummary;

@RestController @RequestMapping("/api/ledger/accounts") @RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    @GetMapping("/me")
    public AccountSummary current(Authentication authentication) {
        return accountService.current(authentication);
    }

    @GetMapping("/{accountId}/recipient")
    public RecipientSummary recipient(Authentication authentication, @PathVariable Long accountId) {
        return accountService.recipient(authentication, accountId);
    }

    @PostMapping
    public AccountSummary open(Authentication authentication, @Valid @RequestBody OpenAccountRequest request) {
        return accountService.open(authentication, request);
    }
}
