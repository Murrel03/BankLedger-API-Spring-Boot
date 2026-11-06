package com.murrel.bankledger.service;

import com.murrel.bankledger.dto.AccountResponse;
import com.murrel.bankledger.dto.CreateAccountRequest;
import com.murrel.bankledger.dto.TransactionResponse;
import com.murrel.bankledger.dto.TransferRequest;
import com.murrel.bankledger.exception.AccountNotFoundException;
import com.murrel.bankledger.exception.InsufficientFundsException;
import com.murrel.bankledger.model.Account;
import com.murrel.bankledger.model.Transaction;
import com.murrel.bankledger.model.TransactionType;
import com.murrel.bankledger.repository.AccountRepository;
import com.murrel.bankledger.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * All business logic lives here. Controllers stay thin (parse HTTP, delegate,
 * return), repositories only talk to the database.
 *
 * <p>Note the pattern below: because methods are {@code @Transactional} and
 * the entities were loaded inside that transaction, mutating a managed
 * entity (calling {@code deposit}/{@code withdraw}) is enough — JPA's dirty
 * checking writes the change on commit, no explicit {@code save()} needed.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // Constructor injection: dependencies are explicit and the class is easy
    // to unit-test with mocks. Spring calls this constructor automatically.
    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account(
                request.accountHolderName(),
                generateAccountNumber(),
                request.openingBalance());
        Account saved = accountRepository.save(account);

        if (request.openingBalance().compareTo(BigDecimal.ZERO) > 0) {
            transactionRepository.save(new Transaction(
                    saved, TransactionType.DEPOSIT,
                    request.openingBalance(), saved.getBalance(), null));
        }
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        return AccountResponse.from(findAccount(id));
    }

    @Transactional
    public AccountResponse deposit(Long id, BigDecimal amount) {
        Account account = findAccount(id);
        account.deposit(amount);
        transactionRepository.save(new Transaction(
                account, TransactionType.DEPOSIT, amount, account.getBalance(), null));
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse withdraw(Long id, BigDecimal amount) {
        Account account = findAccount(id);
        requireSufficientFunds(account, amount);
        account.withdraw(amount);
        transactionRepository.save(new Transaction(
                account, TransactionType.WITHDRAWAL, amount, account.getBalance(), null));
        return AccountResponse.from(account);
    }

    /**
     * Moves money between two accounts atomically. The whole method runs in
     * ONE database transaction: if the second update (or anything else)
     * fails, the withdrawal from the source account is rolled back too, so
     * money can never disappear mid-transfer.
     */
    @Transactional
    public void transfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        Account from = findAccount(request.fromAccountId());
        Account to = findAccount(request.toAccountId());

        requireSufficientFunds(from, request.amount());

        from.withdraw(request.amount());
        to.deposit(request.amount());

        transactionRepository.save(new Transaction(
                from, TransactionType.TRANSFER_OUT, request.amount(),
                from.getBalance(), to.getAccountNumber()));
        transactionRepository.save(new Transaction(
                to, TransactionType.TRANSFER_IN, request.amount(),
                to.getBalance(), from.getAccountNumber()));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long accountId) {
        findAccount(accountId); // 404 if the account does not exist
        return transactionRepository.findByAccountIdOrderByIdDesc(accountId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    private Account findAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void requireSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getId(), account.getBalance(), amount);
        }
    }

    private String generateAccountNumber() {
        long number = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        return "ACC" + number;
    }
}
