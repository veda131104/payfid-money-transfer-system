package com.company.mts.controller;

import com.company.mts.dto.AccountSetupRequest;
import com.company.mts.dto.AccountSetupResponse;
import com.company.mts.entity.Account;
import com.company.mts.entity.AccountStatus;
import com.company.mts.entity.BankDetails;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.AuthUserRepository;
import com.company.mts.service.BankDetailsService;
import com.company.mts.service.EmailService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/v1/account-setup")
@Slf4j
public class AccountSetupController {

    private final BankDetailsService service;
    private final EmailService emailService;
    private final AccountRepository accountRepository;
    private final AuthUserRepository authUserRepository;
    // Simple in-memory OTP store for demo purposes
    private final Map<String, String> otpStore = new HashMap<>();
    private final Set<String> verifiedContacts = ConcurrentHashMap.newKeySet();

    public AccountSetupController(BankDetailsService service, EmailService emailService,
            AccountRepository accountRepository, AuthUserRepository authUserRepository) {
        this.service = service;
        this.emailService = emailService;
        this.accountRepository = accountRepository;
        this.authUserRepository = authUserRepository;
        log.info("[AccountSetupController] Initialized with BankDetailsService, EmailService, AccountRepository, AuthUserRepository");
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AccountSetupRequest request) {
        log.info("[AccountSetupController] POST / - Received account setup request for userName='{}', accountNumber='{}', bankName='{}'",
                request.getUserName(), request.getAccountNumber(), request.getBankName());
        try {
            // Ensure userName is present: prefer request value, otherwise try to infer from the security context (JWT)
            if (request.getUserName() == null || request.getUserName().isBlank()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getName() != null && !auth.getName().isBlank()) {
                    request.setUserName(auth.getName());
                    log.info("[AccountSetupController] POST / - Inferred userName '{}' from security context", auth.getName());
                }
            }

            // If still missing, reject the request
            if (request.getUserName() == null || request.getUserName().isBlank()) {
                log.warn("[AccountSetupController] POST / - REJECTED: Missing userName in request and security context");
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not logged in or session expired. Please log in and try again.");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            String userEmail = normalizeContact(request.getEmail());
            if (userEmail == null || !verifiedContacts.contains(userEmail)) {
                log.warn("[AccountSetupController] POST / - REJECTED: Email '{}' has not been verified via OTP", request.getEmail());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email verification required. Please verify the OTP sent to your email before completing setup.");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            verifiedContacts.remove(userEmail);

            // Generate UPI ID from userName (always unique) instead of email prefix
            String generatedUpiId = generateUpiId(request.getUserName(), request.getBankName());
            log.debug("[AccountSetupController] POST / - Generated UPI ID: '{}'", generatedUpiId);

            // Check if bank details already exist for this user or account number (self-healing mode)
            Optional<BankDetails> existingDetails = service.findByUserName(request.getUserName());
            if (existingDetails.isEmpty()) {
                existingDetails = service.findByAccountNumber(request.getAccountNumber());
            }

            BankDetails saved;
            if (existingDetails.isPresent()) {
                saved = existingDetails.get();
                log.info("[AccountSetupController] POST / - Found existing bank details for user='{}' or account='{}'",
                        saved.getUserName(), saved.getAccountNumber());
                // Verify if it belongs to the same user and account number
                if (!saved.getUserName().equalsIgnoreCase(request.getUserName()) || !saved.getAccountNumber().equals(request.getAccountNumber())) {
                    log.warn("[AccountSetupController] POST / - CONFLICT: Bank details belong to different user/account. Existing user='{}', request user='{}'",
                            saved.getUserName(), request.getUserName());
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "Account setup already exists for this user or account number. Please contact support.");
                    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
                }
                log.info("[AccountSetupController] POST / - Re-using existing bank details (self-healing). id={}", saved.getId());
            } else {
                log.info("[AccountSetupController] POST / - Creating new bank details for userName='{}'", request.getUserName());
                BankDetails details = BankDetails.builder()
                        .accountNumber(request.getAccountNumber())
                        .bankName(request.getBankName())
                        .ifscCode(request.getIfscCode())
                        .branchName(request.getBranchName())
                        .address(request.getAddress())
                        .email(request.getEmail())
                        .phoneNumber(request.getPhoneNumber())
                        .userName(request.getUserName())
                        .creditCardNumber(request.getCreditCardNumber())
                        .cvv(request.getCvv())
                        .expiryDate(request.getExpiryDate())
                        .upiId(generatedUpiId)
                        .build();

                saved = service.save(details);
                log.info("[AccountSetupController] POST / - Bank details SAVED. id={}, accountNumber='{}', upiId='{}'",
                        saved.getId(), saved.getAccountNumber(), saved.getUpiId());
            }

            // SYNC: Create an Account entry with ₹10,000 initial balance if it doesn't exist
            if (!accountRepository.existsByAccountNumber(saved.getAccountNumber())) {
                log.info("[AccountSetupController] POST / - Creating Account entry with ₹10,000 for accountNumber='{}'",
                        saved.getAccountNumber());
                Account account = Account.builder()
                        .accountNumber(saved.getAccountNumber())
                        .holderName(saved.getUserName())
                        .balance(new java.math.BigDecimal("10000.00"))
                        .status(AccountStatus.ACTIVE)
                        .build();
                accountRepository.save(account);
                log.info("[AccountSetupController] POST / - Account entry CREATED for accountNumber='{}'", saved.getAccountNumber());
            } else {
                log.info("[AccountSetupController] POST / - Account entry already exists for accountNumber='{}'", saved.getAccountNumber());
            }

            // Update AuthUser firstLogin flag to false
            authUserRepository.findByNameIgnoreCase(saved.getUserName()).ifPresent(user -> {
                if (user.isFirstLogin()) {
                    user.setFirstLogin(false);
                    authUserRepository.save(user);
                    log.info("[AccountSetupController] POST / - Updated AuthUser '{}' firstLogin flag to false", saved.getUserName());
                }
            });

            AccountSetupResponse resp = new AccountSetupResponse(saved.getId(), saved.getAccountNumber(),
                    saved.getUpiId());
            log.info("[AccountSetupController] POST / - Account setup SUCCESS. Returning response: id={}, accountNumber='{}', upiId='{}'",
                    resp.getId(), resp.getAccountNumber(), resp.getUpiId());
            return new ResponseEntity<>(resp, HttpStatus.CREATED);

        } catch (DataIntegrityViolationException e) {
            log.error("[AccountSetupController] POST / - DataIntegrityViolationException: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Account setup already exists for this user or account number. Please contact support.");
            return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("[AccountSetupController] POST / - Unexpected error: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during account setup.");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateUpiId(String userName, String bankName) {
        if (userName == null || bankName == null)
            return null;

        // Use userName (always unique) as UPI prefix to avoid conflicts
        String prefix = userName.toLowerCase().replaceAll("[^a-z0-9]", "");
        String suffix = "@payfid";

        String upiId = prefix + suffix;
        log.debug("[AccountSetupController] generateUpiId: userName='{}', bankName='{}' => upiId='{}'", userName, bankName, upiId);
        return upiId;
    }

    private String normalizeContact(String contact) {
        if (contact == null || contact.isBlank()) {
            return null;
        }
        return contact.trim().toLowerCase();
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<BankDetails> getByAccountNumber(@PathVariable String accountNumber) {
        log.info("[AccountSetupController] GET /{} - Looking up bank details by account number", accountNumber);
        BankDetails details = service.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("[AccountSetupController] GET /{} - Bank details NOT FOUND", accountNumber);
                    return new IllegalArgumentException("Not found");
                });
        log.info("[AccountSetupController] GET /{} - Bank details FOUND. id={}, userName='{}'",
                accountNumber, details.getId(), details.getUserName());
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{id}/upi")
    public ResponseEntity<AccountSetupResponse> setupUpi(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String upiId = body.get("upiId");
        log.info("[AccountSetupController] POST /{}/upi - Setting up UPI ID: '{}'", id, upiId);
        BankDetails updated = service.setupUpi(id, upiId);
        log.info("[AccountSetupController] POST /{}/upi - UPI setup SUCCESS. upiId='{}'", id, updated.getUpiId());
        return ResponseEntity
                .ok(new AccountSetupResponse(updated.getId(), updated.getAccountNumber(), updated.getUpiId()));
    }

    @GetMapping("/user/{userName}")
    public ResponseEntity<BankDetails> getByUserName(@PathVariable String userName) {
        log.info("[AccountSetupController] GET /user/{} - Looking up bank details by userName", userName);
        BankDetails details = service.findByUserName(userName)
                .orElseThrow(() -> {
                    log.warn("[AccountSetupController] GET /user/{} - Bank details NOT FOUND", userName);
                    return new IllegalArgumentException("Bank details not found for user: " + userName);
                });
        log.info("[AccountSetupController] GET /user/{} - Bank details FOUND. id={}, accountNumber='{}'",
                userName, details.getId(), details.getAccountNumber());
        return ResponseEntity.ok(details);
    }

    @PutMapping("/user/{userName}")
    public ResponseEntity<BankDetails> update(@PathVariable String userName, @RequestBody AccountSetupRequest request) {
        log.info("[AccountSetupController] PUT /user/{} - Updating bank details", userName);
        BankDetails existing = service.findByUserName(userName)
                .orElseThrow(() -> {
                    log.warn("[AccountSetupController] PUT /user/{} - Bank details NOT FOUND", userName);
                    return new IllegalArgumentException("Bank details not found for user: " + userName);
                });

        existing.setBankName(request.getBankName());
        existing.setIfscCode(request.getIfscCode());
        existing.setBranchName(request.getBranchName());
        existing.setAddress(request.getAddress());
        existing.setEmail(request.getEmail());
        existing.setPhoneNumber(request.getPhoneNumber());
        existing.setCreditCardNumber(request.getCreditCardNumber());
        existing.setCvv(request.getCvv());
        existing.setExpiryDate(request.getExpiryDate());

        BankDetails saved = service.save(existing);
        log.info("[AccountSetupController] PUT /user/{} - Bank details UPDATED. id={}", userName, saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/user/{userName}/pin")
    public ResponseEntity<BankDetails> setPin(@PathVariable String userName, @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        log.info("[AccountSetupController] POST /user/{}/pin - Setting PIN (length={})", userName, pin != null ? pin.length() : 0);
        if (pin == null || pin.length() != 4) {
            log.warn("[AccountSetupController] POST /user/{}/pin - REJECTED: Invalid PIN length", userName);
            throw new IllegalArgumentException("PIN must be 4 digits");
        }
        BankDetails updated = service.updatePin(userName, pin);
        log.info("[AccountSetupController] POST /user/{}/pin - PIN set SUCCESS", userName);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String contact = normalizeContact(body.get("contact"));
        log.info("[AccountSetupController] POST /send-otp - Sending OTP to contact='{}'", contact);
        if (contact == null || contact.isBlank()) {
            Map<String, Object> bad = new HashMap<>();
            bad.put("sent", false);
            bad.put("message", "Contact is required to send OTP.");
            return new ResponseEntity<>(bad, HttpStatus.BAD_REQUEST);
        }
        verifiedContacts.remove(contact);
        // generate simple 6-digit OTP
        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        otpStore.put(contact, otp);
        log.debug("[AccountSetupController] POST /send-otp - Generated OTP for contact='{}' (stored in memory)", contact);

        // Send real email if contact looks like an email address
        if (contact.contains("@")) {
            log.info("[AccountSetupController] POST /send-otp - Contact is an email, sending OTP email to '{}'", contact);
            try {
                emailService.sendOtpEmail(contact, otp);
                log.info("[AccountSetupController] POST /send-otp - OTP email sent successfully to '{}'", contact);
            } catch (Exception e) {
                log.error("[AccountSetupController] POST /send-otp - Failed to send OTP email to '{}': {} - {}",
                        contact, e.getClass().getSimpleName(), e.getMessage());
                // Don't fail the entire request if email fails - store OTP anyway for testing
                log.warn("[AccountSetupController] POST /send-otp - OTP stored in memory despite email failure. For testing, check logs.");
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("sent", true);
        resp.put("contact", contact);
        // OTP is NOT returned in response - it is sent to user's email only
        log.info("[AccountSetupController] POST /send-otp - Response sent. OTP generated for contact='{}'", contact);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String contact = normalizeContact(body.get("contact"));
        String otp = body.get("otp");
        log.info("[AccountSetupController] POST /verify-otp - Verifying OTP for contact='{}', otp='{}'", contact, otp);
        boolean ok = contact != null && otp != null && otp.equals(otpStore.get(contact));
        Map<String, Object> resp = new HashMap<>();
        resp.put("verified", ok);
        if (ok) {
            otpStore.remove(contact);
            verifiedContacts.add(contact);
            log.info("[AccountSetupController] POST /verify-otp - OTP verification SUCCESS for contact='{}'", contact);
        } else {
            log.warn("[AccountSetupController] POST /verify-otp - OTP verification FAILED for contact='{}'. Expected='{}', Got='{}'",
                    contact, otpStore.get(contact), otp);
        }
        return ResponseEntity.ok(resp);
    }
}
