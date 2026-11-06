package com.murrel.bankledger.dto;

import com.murrel.bankledger.model.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What the API returns for an account. Using a DTO (instead of returning the
 * JPA entity) controls exactly which fields leave the server and avoids
 * lazy-loading / serialisation surprises.
 */
public record AccountResponse(
        Long id,
        String accountHolderName,
        String accountNumber,
        BigDecimal balance,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountHolderName(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCreatedAt());
    }
}
