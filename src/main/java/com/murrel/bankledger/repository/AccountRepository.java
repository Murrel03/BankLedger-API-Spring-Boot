package com.murrel.bankledger.repository;

import com.murrel.bankledger.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the implementation of this interface at runtime —
 * CRUD methods come free from {@link JpaRepository}, and any finder we add
 * later (e.g. {@code findByAccountNumber}) would be derived from its name.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {
}
