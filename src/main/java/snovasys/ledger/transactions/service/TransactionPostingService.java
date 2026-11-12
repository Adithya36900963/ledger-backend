package snovasys.ledger.transactions.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.entity.BankAccount;
import snovasys.ledger.transactions.entity.LedgerEntry;
import snovasys.ledger.transactions.model.TransactionRequest;
import snovasys.ledger.transactions.repository.BankAccountRepository;
import snovasys.ledger.transactions.repository.LedgerEntryRepository;
import snovasys.ledger.audit.service.AuditLogService;
import snovasys.ledger.users.entity.ACCOUNT_HOLDER;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.repository.ACCOUNT_HOLDERRepo;
import snovasys.ledger.users.service.MpinService;

@Service
@RequiredArgsConstructor
public class TransactionPostingService {
    private final BankAccountRepository accounts;
    private final LedgerEntryRepository entries;
    private final ACCOUNT_HOLDERRepo holders;
    private final AuditLogService auditLog;
    private final MpinService mpinService;

    @Transactional
    public TransactionResult deposit(Authentication auth, TransactionRequest request) {
        User user = authenticated(auth);
        BankAccount account;
        if (user.getRole() == Role.ADMIN) {
            if (request.getDestinationAccountId() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destinationAccountId is required for teller deposits");
            account = locked(request.getDestinationAccountId());
            if (account.isSystemAccount())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teller deposits must target a customer account");
        } else {
            requireRole(user, Role.ACCOUNT_HOLDER);
            account = ownAccount(user, true);
        }
        BigDecimal amount = amount(request);
        Instant now = Instant.now();
        String tx = UUID.randomUUID().toString();
        account.setBalance(account.getBalance().add(amount));
        entries.save(entry(tx, account, amount, "DEPOSIT", now));
        BankAccount cash = cashAccount();
        cash.setBalance(cash.getBalance().subtract(amount));
        entries.save(entry(tx, cash, amount.negate(), "DEPOSIT", now));
        auditLog.record(auth, "DEPOSIT", "POSTED", "transactionId=" + tx + ", accountId=" + account.getId() + ", amount=" + amount);
        return new TransactionResult(tx, "DEPOSIT", null, account.getId(), amount, account.getBalance(), now);
    }

    @Transactional
    public TransactionResult withdraw(Authentication auth, TransactionRequest request) {
        User user = authenticated(auth);
        requireRole(user, Role.ACCOUNT_HOLDER);
        BankAccount account = ownAccount(user, true);
        BigDecimal amount = amount(request);
        ensureFunds(account, amount);
        Instant now = Instant.now();
        String tx = UUID.randomUUID().toString();
        account.setBalance(account.getBalance().subtract(amount));
        entries.save(entry(tx, account, amount.negate(), "WITHDRAWAL", now));
        BankAccount cash = cashAccount();
        cash.setBalance(cash.getBalance().add(amount));
        entries.save(entry(tx, cash, amount, "WITHDRAWAL", now));
        auditLog.record(auth, "WITHDRAWAL", "POSTED", "transactionId=" + tx + ", accountId=" + account.getId() + ", amount=" + amount);
        return new TransactionResult(tx, "WITHDRAWAL", account.getId(), null, amount, account.getBalance(), now);
    }

    @Transactional
    public TransactionResult transfer(Authentication auth, TransactionRequest request) {
        User user = authenticated(auth);
        requireRole(user, Role.ACCOUNT_HOLDER);
        mpinService.verify(user, request.getMpin());
        if (request.getDestinationAccountId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destinationAccountId is required");
        BankAccount source = ownAccount(user, false);
        if (source.getId().equals(request.getDestinationAccountId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must differ");
        // Lock accounts in ascending order to reduce deadlock risk for opposing transfers.
        Long firstId = Math.min(source.getId(), request.getDestinationAccountId());
        Long secondId = Math.max(source.getId(), request.getDestinationAccountId());
        BankAccount first = locked(firstId);
        BankAccount second = locked(secondId);
        if (first.isSystemAccount() || second.isSystemAccount())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfers must use customer accounts");
        source = source.getId().equals(firstId) ? first : second;
        BankAccount destination = source.getId().equals(firstId) ? second : first;
        BigDecimal amount = amount(request);
        ensureFunds(source, amount);
        Instant now = Instant.now();
        String tx = UUID.randomUUID().toString();
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        entries.save(entry(tx, source, amount.negate(), "TRANSFER", now));
        entries.save(entry(tx, destination, amount, "TRANSFER", now));
        auditLog.record(auth, "TRANSFER", "POSTED", "transactionId=" + tx + ", fromAccountId=" + source.getId()
                + ", toAccountId=" + destination.getId() + ", amount=" + amount);
        return new TransactionResult(tx, "TRANSFER", source.getId(), destination.getId(), amount, source.getBalance(), now);
    }

    private User authenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return user;
    }

    private void requireRole(User user, Role role) {
        if (user.getRole() != role)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This operation is not allowed for your role");
    }

    private BankAccount ownAccount(User user, boolean lock) {
        ACCOUNT_HOLDER holder = holders.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account holder profile not found"));
        Long id = accounts.findByHolder_Id(holder.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found")).getId();
        return lock ? locked(id) : accounts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found"));
    }

    private BankAccount locked(Long id) {
        return accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found"));
    }

    private BankAccount cashAccount() {
        BankAccount cash = accounts.findBySystemAccountTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "System cash account is not initialized"));
        return locked(cash.getId());
    }

    private BigDecimal amount(TransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0 || request.getAmount().scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive and have at most two decimal places");
        return request.getAmount().setScale(2, RoundingMode.UNNECESSARY);
    }

    private void ensureFunds(BankAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
    }

    private LedgerEntry entry(String tx, BankAccount account, BigDecimal amount, String type, Instant now) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransactionId(tx);
        entry.setAccount(account);
        entry.setAmount(amount);
        entry.setType(type);
        entry.setCreatedAt(now);
        return entry;
    }
}
