package snovasys.ledger.transactions.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.model.LedgerEntryView;
import snovasys.ledger.transactions.service.LedgerReadService;

@RestController @RequestMapping("/api/ledger/entries") @RequiredArgsConstructor
public class LedgerReadController {
    private final LedgerReadService ledgerReadService;
    @GetMapping("/me")
    public List<LedgerEntryView> myEntries(Authentication authentication) { return ledgerReadService.myEntries(authentication); }
}
