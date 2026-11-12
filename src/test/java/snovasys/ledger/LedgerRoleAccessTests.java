package snovasys.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import snovasys.ledger.audit.service.AuditLogService;
import snovasys.ledger.transactions.model.OpenAccountRequest;
import snovasys.ledger.transactions.model.TransactionRequest;
import snovasys.ledger.transactions.repository.BankAccountRepository;
import snovasys.ledger.transactions.repository.LedgerEntryRepository;
import snovasys.ledger.transactions.service.AccountService;
import snovasys.ledger.transactions.service.LedgerReadService;
import snovasys.ledger.transactions.service.TransactionPostingService;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;
import snovasys.ledger.users.repository.ACCOUNT_HOLDERRepo;
import snovasys.ledger.users.service.MpinService;

class LedgerRoleAccessTests {
    private final BankAccountRepository accounts = mock(BankAccountRepository.class);
    private final LedgerEntryRepository entries = mock(LedgerEntryRepository.class);
    private final ACCOUNT_HOLDERRepo holders = mock(ACCOUNT_HOLDERRepo.class);
    private final AuditLogService auditLog = mock(AuditLogService.class);
    private final MpinService mpinService = mock(MpinService.class);

    @Test
    void auditorCannotOpenAnAccount() {
        AccountService service = new AccountService(accounts, holders, auditLog);
        OpenAccountRequest request = new OpenAccountRequest();
        request.setAccountHolderId(12L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.open(authentication(Role.AUDITOR), request));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(accounts, holders, auditLog);
    }

    @Test
    void auditorCannotPostAWithdrawal() {
        TransactionPostingService service = new TransactionPostingService(accounts, entries, holders, auditLog, mpinService);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.withdraw(authentication(Role.AUDITOR), mock(TransactionRequest.class)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(accounts, entries, holders, auditLog, mpinService);
    }

    @Test
    void auditorCannotReadHolderScopedLedgerHistory() {
        LedgerReadService service = new LedgerReadService(entries, accounts, holders);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.myEntries(authentication(Role.AUDITOR)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(entries, accounts, holders);
    }

    private static Authentication authentication(Role role) {
        User user = User.builder().id(7L).username("test-user").role(role).build();
        return UsernamePasswordAuthenticationToken.authenticated(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }
}
