package com.murrel.bankledger.repository;

import com.murrel.bankledger.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Derived query: Spring Data parses the method name into a WHERE clause.
     *  Ordered by id (insertion order) so history is deterministic even when
     *  two entries land in the same millisecond. */
    List<Transaction> findByAccountIdOrderByIdDesc(Long accountId);
}
