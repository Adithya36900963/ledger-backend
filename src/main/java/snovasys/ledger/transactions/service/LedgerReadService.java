package snovasys.ledger.transactions.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.entity.LedgerEntry;
import snovasys.ledger.transactions.model.LedgerEntryView;
import snovasys.ledger.transactions.model.LedgerIntegrityView;
import snovasys.ledger.transactions.model.TransferHistoryView;
import snovasys.ledger.transactions.repository.BankAccountRepository;
import snovasys.ledger.transactions.repository.LedgerEntryRepository;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.repository.ACCOUNT_HOLDERRepo;

@Service @RequiredArgsConstructor
public class LedgerReadService {
    private final LedgerEntryRepository entries;
    private final BankAccountRepository accounts;
    private final ACCOUNT_HOLDERRepo holders;

    @Transactional(readOnly = true)
    public List<LedgerEntryView> allEntries() {
        return entries.findTop200ByOrderByCreatedAtDescIdDesc().stream().map(LedgerReadService::view).toList();
    }

    @Transactional(readOnly = true)
    public LedgerIntegrityView integrity() {
        var total = entries.sumAllEntries();
        long customerAccounts = accounts.countBySystemAccountFalse();
        long negativeBalances = accounts.countBySystemAccountFalseAndBalanceLessThan(java.math.BigDecimal.ZERO);
        return new LedgerIntegrityView(total.compareTo(java.math.BigDecimal.ZERO) == 0 && negativeBalances == 0,
                total, customerAccounts, negativeBalances);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryView> myEntries(Authentication authentication) {
        User user = requireUser(authentication);
        if (user.getRole() != Role.ACCOUNT_HOLDER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account holder access required");
        var holder = holders.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account holder profile not found"));
        var account = accounts.findByHolder_Id(holder.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found"));
        return entries.findByAccount_IdOrderByCreatedAtDescIdDesc(account.getId()).stream()
                .map(LedgerReadService::view).toList();
    }

    @Transactional(readOnly = true)
    public List<TransferHistoryView> myTransfers(Authentication authentication) {
        User user = requireUser(authentication);
        if (user.getRole() != Role.ACCOUNT_HOLDER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account holder access required");
        var holder = holders.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account holder profile not found"));
        var account = accounts.findByHolder_Id(holder.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger account not found"));
        return entries.findByAccount_IdAndTypeOrderByCreatedAtDescIdDesc(account.getId(), "TRANSFER").stream()
                .map(entry -> entries.findFirstByTransactionIdAndAccount_IdNot(entry.getTransactionId(), account.getId())
                        .map(counterparty -> {
                            boolean sent = entry.getAmount().signum() < 0;
                            var from = sent ? entry.getAccount() : counterparty.getAccount();
                            var to = sent ? counterparty.getAccount() : entry.getAccount();
                            String name = counterparty.getAccount().isSystemAccount() ? "System account"
                                    : counterparty.getAccount().getHolder().getUser().getUsername();
                            return new TransferHistoryView(entry.getTransactionId(), from.getId(), to.getId(),
                                    counterparty.getAccount().getId(), name, entry.getAmount().abs(), entry.getCreatedAt());
                        }).orElse(null))
                .filter(java.util.Objects::nonNull).toList();
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return user;
    }

    private static LedgerEntryView view(LedgerEntry entry) {
        return new LedgerEntryView(entry.getId(), entry.getTransactionId(), entry.getAccount().getId(),
                entry.getAccount().isSystemAccount() ? "System cash" : entry.getAccount().getHolder().getUser().getUsername(),
                entry.getAmount(), entry.getType(), entry.getCreatedAt());
    }
}
