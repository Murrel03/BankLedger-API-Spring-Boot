package com.murrel.bankledger.controller;

import com.murrel.bankledger.dto.AccountResponse;
import com.murrel.bankledger.dto.AmountRequest;
import com.murrel.bankledger.dto.CreateAccountRequest;
import com.murrel.bankledger.dto.TransactionResponse;
import com.murrel.bankledger.dto.TransferRequest;
import com.murrel.bankledger.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP layer only: parse the request, delegate to the service, return a DTO.
 * {@code @Valid} triggers bean validation on the request DTOs; failures are
 * turned into a 400 by {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping
    public List<AccountResponse> listAccounts() {
        return accountService.listAccounts();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return accountService.deposit(id, request.amount());
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return accountService.withdraw(id, request.amount());
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transfer(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> getTransactionHistory(@PathVariable Long id) {
        return accountService.getTransactionHistory(id);
    }
}
