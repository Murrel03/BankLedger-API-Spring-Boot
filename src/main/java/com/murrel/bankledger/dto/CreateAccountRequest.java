package com.murrel.bankledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * What a client must send to open an account. Validation lives here, on the
 * DTO, so an invalid request is rejected before the service layer runs.
 */
public record CreateAccountRequest(

        @NotBlank(message = "accountHolderName is required")
        String accountHolderName,

        @NotNull(message = "openingBalance is required")
        @DecimalMin(value = "0.00", message = "openingBalance cannot be negative")
        BigDecimal openingBalance
) {
}
