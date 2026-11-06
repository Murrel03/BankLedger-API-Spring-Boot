package com.murrel.bankledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Body for a transfer between two account ids. */
public record TransferRequest(

        @NotNull(message = "fromAccountId is required")
        Long fromAccountId,

        @NotNull(message = "toAccountId is required")
        Long toAccountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount
) {
}
