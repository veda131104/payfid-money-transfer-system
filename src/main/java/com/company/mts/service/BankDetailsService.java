package com.company.mts.service;

import com.company.mts.entity.BankDetails;
import com.company.mts.repository.BankDetailsRepository;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Service
@Slf4j
public class BankDetailsService {

    private final BankDetailsRepository repository;

    public BankDetailsService(BankDetailsRepository repository) {
        this.repository = repository;
        log.info("[BankDetailsService] Initialized with BankDetailsRepository");
    }

    public BankDetails save(BankDetails details) {
        log.info("[BankDetailsService] save: Saving bank details for userName='{}', accountNumber='{}'",
                details.getUserName(), details.getAccountNumber());
        BankDetails saved = repository.save(details);
        log.info("[BankDetailsService] save: Bank details SAVED. id={}", saved.getId());
        return saved;
    }

    public Optional<BankDetails> findByAccountNumber(String accountNumber) {
        log.debug("[BankDetailsService] findByAccountNumber: Looking up accountNumber='{}'", accountNumber);
        Optional<BankDetails> result = repository.findByAccountNumber(accountNumber);
        log.debug("[BankDetailsService] findByAccountNumber: Found={}", result.isPresent());
        return result;
    }

    public Optional<BankDetails> findById(Long id) {
        log.debug("[BankDetailsService] findById: Looking up id={}", id);
        Optional<BankDetails> result = repository.findById(id);
        log.debug("[BankDetailsService] findById: Found={}", result.isPresent());
        return result;
    }

    public Optional<BankDetails> findByUserName(String userName) {
        log.debug("[BankDetailsService] findByUserName: Looking up userName='{}'", userName);
        Optional<BankDetails> result = repository.findByUserName(userName);
        log.debug("[BankDetailsService] findByUserName: Found={}", result.isPresent());
        return result;
    }

    public BankDetails setupUpi(Long id, String upiId) {
        log.info("[BankDetailsService] setupUpi: Setting UPI id={}, upiId='{}'", id, upiId);
        BankDetails details = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[BankDetailsService] setupUpi: Bank details NOT FOUND for id={}", id);
                    return new IllegalArgumentException("Bank details not found");
                });
        details.setUpiId(upiId);
        BankDetails saved = repository.save(details);
        log.info("[BankDetailsService] setupUpi: UPI set SUCCESS. id={}, upiId='{}'", id, upiId);
        return saved;
    }

    public BankDetails updatePin(String userName, String pin) {
        log.info("[BankDetailsService] updatePin: Updating PIN for userName='{}'", userName);
        BankDetails details = repository.findByUserName(userName)
                .orElseThrow(() -> {
                    log.warn("[BankDetailsService] updatePin: Bank details NOT FOUND for userName='{}'", userName);
                    return new IllegalArgumentException("Bank details not found for user: " + userName);
                });
        details.setPin(pin);
        BankDetails saved = repository.save(details);
        log.info("[BankDetailsService] updatePin: PIN updated for userName='{}'", userName);
        return saved;
    }
}
