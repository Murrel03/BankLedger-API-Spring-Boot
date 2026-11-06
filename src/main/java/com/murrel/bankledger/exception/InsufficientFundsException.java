package com.murrel.bankledger.exception;

import java.math.BigDecimal;

/**
 * Business-rule violation: a withdrawal or transfer exceeds the balance.
 * Mapped to HTTP 422. Because it is a RuntimeException, any database work
 * done earlier in the same @Transactional method is rolled back.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long accountId, BigDecimal balance, BigDecimal attempted) {
        super("Insufficient funds on account " + accountId
                + ": balance is " + balance + " but " + attempted + " was requested");
    }
}
