package snovasys.ledger.transactions.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.model.TransactionRequest;

@Service @RequiredArgsConstructor
public class DepositService {
    private final TransactionPostingService posting;
    public TransactionResult deposit(Authentication auth, TransactionRequest request) { return posting.deposit(auth, request); }
}
