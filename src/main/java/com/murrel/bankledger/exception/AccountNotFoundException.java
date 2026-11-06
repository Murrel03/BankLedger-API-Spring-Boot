package com.murrel.bankledger.exception;

/** Thrown when an account id does not exist. Mapped to HTTP 404. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Account not found with id: " + id);
    }
}
