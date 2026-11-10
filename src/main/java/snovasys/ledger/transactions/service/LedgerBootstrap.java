package snovasys.ledger.transactions.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import snovasys.ledger.transactions.entity.BankAccount;
import snovasys.ledger.transactions.repository.BankAccountRepository;

@Component
@RequiredArgsConstructor
public class LedgerBootstrap implements ApplicationRunner {
    private final BankAccountRepository accounts;

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.findBySystemAccountTrue().isEmpty()) {
            BankAccount cash = new BankAccount();
            cash.setSystemAccount(true);
            accounts.save(cash);
        }
    }
}
