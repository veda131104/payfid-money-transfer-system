package com.company.mts.service;

import com.company.mts.entity.BankDetails;
import com.company.mts.repository.BankDetailsRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BankDetailsService {

    private final BankDetailsRepository repository;

    public BankDetailsService(BankDetailsRepository repository) {
        this.repository = repository;
    }

    public BankDetails save(BankDetails details) {
        return repository.save(details);
    }

    public Optional<BankDetails> findByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber);
    }

    public Optional<BankDetails> findById(Long id) {
        return repository.findById(id);
    }

    public BankDetails setupUpi(Long id, String upiId) {
        BankDetails details = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Bank details not found"));
        details.setUpiId(upiId);
        return repository.save(details);
    }
}
