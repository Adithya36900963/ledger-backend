package snovasys.ledger.transactions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import snovasys.ledger.transactions.entity.TransferIdempotency;

public interface TransferIdempotencyRepository extends JpaRepository<TransferIdempotency, String> { }
