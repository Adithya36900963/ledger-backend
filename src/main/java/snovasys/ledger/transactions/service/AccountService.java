package snovasys.ledger.transactions.service;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.entity.BankAccount;
import snovasys.ledger.transactions.model.OpenAccountRequest;
import snovasys.ledger.transactions.repository.BankAccountRepository;
import snovasys.ledger.users.entity.ACCOUNT_HOLDER;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.repository.ACCOUNT_HOLDERRepo;
import snovasys.ledger.audit.service.AuditLogService;

@Service @RequiredArgsConstructor
public class AccountService {
    private final BankAccountRepository accounts;
    private final ACCOUNT_HOLDERRepo holders;
    private final AuditLogService auditLog;

    @Transactional(readOnly = true)
    public AccountSummary current(Authentication auth) {
        User user = authenticated(auth);
        if (user.getRole() != Role.ACCOUNT_HOLDER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account holder role required");
        ACCOUNT_HOLDER holder = holders.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account holder profile not found"));
        BankAccount account = accounts.findByHolder_Id(holder.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found"));
        return new AccountSummary(account.getId(), holder.getId(), holder.getUser().getUsername(), account.getBalance());
    }

    @Transactional
    public AccountSummary open(Authentication auth, OpenAccountRequest request) {
        User user = authenticated(auth);
        if (user.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can open accounts");
        ACCOUNT_HOLDER holder = holders.findById(request.getAccountHolderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account holder not found"));
        if (accounts.findByHolder_Id(holder.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account holder already has a ledger account");
        holder.setStatus(ACCOUNT_HOLDER.ACCOUNT_HOLDERStatus.ACTIVE);
        BankAccount account = new BankAccount();
        account.setHolder(holder);
        account.setSystemAccount(false);
        account.setBalance(BigDecimal.ZERO.setScale(2));
        account = accounts.save(account);
        auditLog.record(auth, "ACCOUNT_OPENED", "SUCCESS", "accountId=" + account.getId()
                + ", accountHolderId=" + holder.getId());
        return new AccountSummary(account.getId(), holder.getId(), holder.getUser().getUsername(), account.getBalance());
    }

    private User authenticated(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return user;
    }

    public record AccountSummary(Long accountId, Long accountHolderId, String holderName, BigDecimal balance) { }
}
