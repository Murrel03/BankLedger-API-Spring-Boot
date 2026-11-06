package com.murrel.bankledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Body for deposit and withdraw: a single strictly-positive amount. */
public record AmountRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount
) {
}
