package snovasys.ledger.transactions.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.model.TransactionRequest;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class TransferService {
    private final TransactionPostingService posting;
    public TransactionResult transfer(Authentication auth, TransactionRequest request, String idempotencyKey) {
        return posting.transfer(auth, request, idempotencyKey);
    }
    public Optional<TransactionResult> replay(Authentication auth, TransactionRequest request, String idempotencyKey) {
        return posting.replayTransfer(auth, request, idempotencyKey);
    }
}
