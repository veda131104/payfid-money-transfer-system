package com.company.mts.service;

import com.company.mts.entity.Account;
import com.company.mts.entity.AccountStatus;
import com.company.mts.exception.DuplicateAccountException;
import com.company.mts.exception.InactiveAccountException;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.AccountRepository;
import com.company.mts.utils.AccountNumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Service
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private static final int MAX_GENERATION_ATTEMPTS = 10;
    private static final BigDecimal MINIMUM_INITIAL_BALANCE = new BigDecimal("100.00");

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        log.info("[AccountService] Initialized with AccountRepository");
    }

    /**
     * Create a new bank account
     * Banking validations:
     * - Check if account already exists for this person (case-insensitive)
     * - Validate holder name is not empty
     * - Validate minimum initial balance
     * - Generate unique account number
     */
    @Transactional
    public Account createAccount(String holderName, BigDecimal initialBalance) {
        log.info("[AccountService] createAccount: holderName='{}', initialBalance={}", holderName, initialBalance);

        // Validation 1: Check holder name
        if (holderName == null || holderName.trim().isEmpty()) {
            log.warn("[AccountService] createAccount: REJECTED - empty holder name");
            throw new IllegalArgumentException("Account holder name cannot be empty");
        }

        // Validation 2: Check if account already exists (case-insensitive)
        if (accountRepository.existsByHolderNameIgnoreCase(holderName.trim())) {
            log.warn("[AccountService] createAccount: DUPLICATE - account already exists for holder '{}'", holderName);
            throw new DuplicateAccountException(
                    "Account already exists for holder: " + holderName + ". " +
                            "A person can only have one account in this banking system."
            );
        }

        // Validation 3: Check minimum initial balance
        if (initialBalance == null) {
            initialBalance = BigDecimal.ZERO;
        }

        if (initialBalance.compareTo(MINIMUM_INITIAL_BALANCE) < 0) {
            log.warn("[AccountService] createAccount: REJECTED - balance {} below minimum {}", initialBalance, MINIMUM_INITIAL_BALANCE);
            throw new IllegalArgumentException(
                    String.format("Minimum initial balance required: %.2f. Provided: %.2f",
                            MINIMUM_INITIAL_BALANCE, initialBalance)
            );
        }

        // Validation 4: Maximum initial deposit limit
        if (initialBalance.compareTo(new BigDecimal("1000000")) > 0) {
            log.warn("[AccountService] createAccount: REJECTED - balance {} exceeds maximum", initialBalance);
            throw new IllegalArgumentException("Initial deposit cannot exceed 1,000,000");
        }

        // Generate unique account number
        String accountNumber = generateUniqueAccountNumber();
        log.info("[AccountService] createAccount: Generated unique account number '{}'", accountNumber);

        // Create account
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .holderName(holderName.trim())
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("[AccountService] createAccount: Account CREATED. id={}, accountNumber='{}', holderName='{}', balance={}",
                saved.getId(), saved.getAccountNumber(), saved.getHolderName(), saved.getBalance());
        return saved;
    }

    /**
     * Generates a unique account number by checking against existing accounts
     */
    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String accountNumber = AccountNumberGenerator.generate();
            log.debug("[AccountService] generateUniqueAccountNumber: Attempt {} - trying '{}'", attempt + 1, accountNumber);

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        log.error("[AccountService] generateUniqueAccountNumber: FAILED after {} attempts", MAX_GENERATION_ATTEMPTS);
        throw new IllegalStateException(
                "Unable to generate unique account number after " + MAX_GENERATION_ATTEMPTS + " attempts. Please try again."
        );
    }

    /**
     * Get account by ID
     */
    public Account getAccount(Long id) {
        log.debug("[AccountService] getAccount: Looking up account by id={}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[AccountService] getAccount: Account NOT FOUND with id={}", id);
                    return new ResourceNotFoundException("Account not found with ID: " + id);
                });
    }

    /**
     * Get account by account number
     */
    public Account getAccountByAccountNumber(String accountNumber) {
        log.debug("[AccountService] getAccountByAccountNumber: Looking up account by number='{}'", accountNumber);
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }

        return accountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> {
                    log.warn("[AccountService] getAccountByAccountNumber: Account NOT FOUND with number='{}'", accountNumber);
                    return new ResourceNotFoundException("Account not found with number: " + accountNumber);
                });
    }

    /**
     * Get account by holder name (case-insensitive)
     */
    public Account getAccountByHolderName(String holderName) {
        log.debug("[AccountService] getAccountByHolderName: Looking up account by holderName='{}'", holderName);
        if (holderName == null || holderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Holder name cannot be empty");
        }

        return accountRepository.findByHolderNameIgnoreCase(holderName.trim())
                .orElseThrow(() -> {
                    log.warn("[AccountService] getAccountByHolderName: Account NOT FOUND for holder='{}'", holderName);
                    return new ResourceNotFoundException("No account found for holder: " + holderName);
                });
    }

    /**
     * Credit (deposit) money to account
     */
    @Transactional
    public Account credit(Long id, BigDecimal amount) {
        log.info("[AccountService] credit: Crediting amount={} to account id={}", amount, id);
        Account account = getAccount(id);
        account.credit(amount);
        log.info("[AccountService] credit: Credit SUCCESS. Account id={}, new balance={}", id, account.getBalance());
        return account;
    }

    /**
     * Debit (withdraw) money from account
     */
    @Transactional
    public Account debit(Long id, BigDecimal amount) {
        log.info("[AccountService] debit: Debiting amount={} from account id={}", amount, id);
        Account account = getAccount(id);
        account.debit(amount);
        log.info("[AccountService] debit: Debit SUCCESS. Account id={}, new balance={}", id, account.getBalance());
        return account;
    }

    /**
     * Transfer money between accounts (by ID)
     */
    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        log.info("[AccountService] transfer: from={}, to={}, amount={}", fromAccountId, toAccountId, amount);

        // Validation 1: Check if transferring to same account
        if (fromAccountId.equals(toAccountId)) {
            log.warn("[AccountService] transfer: REJECTED - same account transfer");
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Validation 2: Check amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[AccountService] transfer: REJECTED - invalid amount={}", amount);
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // Get both accounts
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found with ID: " + fromAccountId));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found with ID: " + toAccountId));

        // Validation 3: Check both accounts are active
        if (!fromAccount.isActive()) {
            throw new InactiveAccountException("Sender account is not active. Status: " + fromAccount.getStatus());
        }

        if (!toAccount.isActive()) {
            throw new InactiveAccountException("Receiver account is not active. Status: " + toAccount.getStatus());
        }

        // Perform transfer (validations happen in debit/credit methods)
        fromAccount.debit(amount);
        toAccount.credit(amount);
        log.info("[AccountService] transfer: Transfer SUCCESS. from={} (balance={}), to={} (balance={})",
                fromAccountId, fromAccount.getBalance(), toAccountId, toAccount.getBalance());
    }

    /**
     * Transfer money using account numbers
     */
    @Transactional
    public void transferByAccountNumber(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        log.info("[AccountService] transferByAccountNumber: from='{}', to='{}', amount={}", fromAccountNumber, toAccountNumber, amount);

        // Validation 1: Check account numbers not empty
        if (fromAccountNumber == null || fromAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender account number cannot be empty");
        }
        if (toAccountNumber == null || toAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Receiver account number cannot be empty");
        }

        // Validation 2: Check if transferring to same account
        if (fromAccountNumber.trim().equalsIgnoreCase(toAccountNumber.trim())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Validation 3: Check amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // Get both accounts
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found: " + fromAccountNumber));

        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found: " + toAccountNumber));

        // Validation 4: Check both accounts are active
        if (!fromAccount.isActive()) {
            throw new InactiveAccountException("Sender account is not active. Status: " + fromAccount.getStatus());
        }

        if (!toAccount.isActive()) {
            throw new InactiveAccountException("Receiver account is not active. Status: " + toAccount.getStatus());
        }

        // Perform transfer
        fromAccount.debit(amount);
        toAccount.credit(amount);
        log.info("[AccountService] transferByAccountNumber: Transfer SUCCESS. from='{}' (balance={}), to='{}' (balance={})",
                fromAccountNumber, fromAccount.getBalance(), toAccountNumber, toAccount.getBalance());
    }

    /**
     * Lock an account (prevent transactions)
     */
    @Transactional
    public Account lockAccount(Long id) {
        log.info("[AccountService] lockAccount: Locking account id={}", id);
        Account account = getAccount(id);
        account.lock();
        log.info("[AccountService] lockAccount: Account id={} LOCKED", id);
        return account;
    }

    /**
     * Unlock/Activate an account
     */
    @Transactional
    public Account unlockAccount(Long id) {
        log.info("[AccountService] unlockAccount: Unlocking account id={}", id);
        Account account = getAccount(id);
        account.activate();
        log.info("[AccountService] unlockAccount: Account id={} ACTIVATED", id);
        return account;
    }

    /**
     * Close an account (must have zero balance)
     */
    @Transactional
    public Account closeAccount(Long id) {
        log.info("[AccountService] closeAccount: Closing account id={}", id);
        Account account = getAccount(id);
        account.close();
        log.info("[AccountService] closeAccount: Account id={} CLOSED", id);
        return account;
    }
}