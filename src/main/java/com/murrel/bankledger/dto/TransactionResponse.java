package com.murrel.bankledger.dto;

import com.murrel.bankledger.model.Transaction;
import com.murrel.bankledger.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What the API returns for one ledger entry. */
public record TransactionResponse(
        Long id,
        Long accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal resultingBalance,
        String relatedAccountNumber,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getResultingBalance(),
                transaction.getRelatedAccountNumber(),
                transaction.getCreatedAt());
    }
}
