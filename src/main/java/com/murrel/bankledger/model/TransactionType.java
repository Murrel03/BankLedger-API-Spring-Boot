package com.murrel.bankledger.model;

/** The kind of money movement a {@link Transaction} records. */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_OUT,
    TRANSFER_IN
}
