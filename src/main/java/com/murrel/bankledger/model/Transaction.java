package com.murrel.bankledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One immutable ledger entry: a single deposit, withdrawal, or one side of
 * a transfer. A transfer creates two entries (TRANSFER_OUT on the source
 * account, TRANSFER_IN on the destination) so each account's history is
 * complete on its own — the same double-entry idea real ledgers use.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Balance of the account immediately after this transaction was applied. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal resultingBalance;

    /** For transfers: the account number on the other side. Null otherwise. */
    private String relatedAccountNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
        // Required by JPA
    }

    public Transaction(Account account, TransactionType type, BigDecimal amount,
                       BigDecimal resultingBalance, String relatedAccountNumber) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
        this.relatedAccountNumber = relatedAccountNumber;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public String getRelatedAccountNumber() {
        return relatedAccountNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
