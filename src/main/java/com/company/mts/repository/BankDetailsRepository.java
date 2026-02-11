package com.company.mts.repository;

import com.company.mts.entity.BankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {
    Optional<BankDetails> findByAccountNumber(String accountNumber);
    Optional<BankDetails> findByUpiId(String upiId);
}
