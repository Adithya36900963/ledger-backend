package snovasys.ledger.admin.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.admin.model.AdminUserView;
import snovasys.ledger.transactions.repository.BankAccountRepository;
import snovasys.ledger.users.entity.ACCOUNT_HOLDER;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.repository.ACCOUNT_HOLDERRepo;
import snovasys.ledger.users.repository.UserRepo;

@Service @RequiredArgsConstructor
public class AdminUserService {
    private final UserRepo users;
    private final ACCOUNT_HOLDERRepo holders;
    private final BankAccountRepository accounts;

    @Transactional(readOnly = true)
    public List<AdminUserView> listUsers() {
        return users.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().limit(200).map(this::view).toList();
    }

    private AdminUserView view(User user) {
        String holderStatus = null;
        Long accountId = null;
        java.math.BigDecimal balance = null;
        if (user.getRole() == Role.ACCOUNT_HOLDER) {
            ACCOUNT_HOLDER holder = holders.findByUser_Id(user.getId()).orElse(null);
            if (holder != null) {
                holderStatus = holder.getStatus() == null ? null : holder.getStatus().name();
                var account = accounts.findByHolder_Id(holder.getId()).orElse(null);
                if (account != null) { accountId = account.getId(); balance = account.getBalance(); }
            }
        }
        return new AdminUserView(user.getId(), user.getUsername(), user.getEmail(), user.getNumber(),
                user.getRole().name(), Boolean.TRUE.equals(user.getEmailVerified()), Boolean.TRUE.equals(user.getMobileVerified()),
                holderStatus, accountId, balance);
    }
}
