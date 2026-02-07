package com.company.mts.repository;

import com.company.mts.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Check if an account number already exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Find an account by account number
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Check if an account exists for a given holder name
     * Used to prevent duplicate accounts for the same person
     */
    boolean existsByHolderName(String holderName);

    /**
     * Find account by holder name
     */
    Optional<Account> findByHolderName(String holderName);

    /**
     * Check if holder name exists (case-insensitive)
     * Prevents "John Doe" and "john doe" from being different accounts
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a WHERE LOWER(a.holderName) = LOWER(:holderName)")
    boolean existsByHolderNameIgnoreCase(@Param("holderName") String holderName);

    /**
     * Find account by holder name (case-insensitive)
     */
    @Query("SELECT a FROM Account a WHERE LOWER(a.holderName) = LOWER(:holderName)")
    Optional<Account> findByHolderNameIgnoreCase(@Param("holderName") String holderName);
}