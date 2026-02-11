package com.company.mts.controller;

import com.company.mts.dto.AccountSetupRequest;
import com.company.mts.dto.AccountSetupResponse;
import com.company.mts.entity.Account;
import com.company.mts.entity.AccountStatus;
import com.company.mts.entity.BankDetails;
import com.company.mts.repository.AccountRepository;
import com.company.mts.service.BankDetailsService;
import com.company.mts.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/account-setup")
public class AccountSetupController {

    private final BankDetailsService service;
    private final EmailService emailService;
    private final AccountRepository accountRepository;
    // Simple in-memory OTP store for demo purposes
    private final Map<String, String> otpStore = new HashMap<>();

    public AccountSetupController(BankDetailsService service, EmailService emailService,
            AccountRepository accountRepository) {
        this.service = service;
        this.emailService = emailService;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<AccountSetupResponse> create(@RequestBody AccountSetupRequest request) {
        String generatedUpiId = generateUpiId(request.getEmail(), request.getBankName());

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
                .upiId(generatedUpiId)
                .build();

        BankDetails saved = service.save(details);

        // SYNC: Create an Account entry if it doesn't exist
        if (!accountRepository.existsByAccountNumber(saved.getAccountNumber())) {
            Account account = Account.builder()
                    .accountNumber(saved.getAccountNumber())
                    .holderName(saved.getUserName())
                    .balance(new java.math.BigDecimal("10000.00")) // Initial balance for new users
                    .status(AccountStatus.ACTIVE)
                    .build();
            accountRepository.save(account);
        }

        AccountSetupResponse resp = new AccountSetupResponse(saved.getId(), saved.getAccountNumber(),
                saved.getUpiId());
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    private String generateUpiId(String contact, String bankName) {
        if (contact == null || bankName == null)
            return null;

        String prefix = contact.split("@")[0];
        String suffix = "@upi";

        String bank = bankName.toUpperCase();
        if (bank.contains("HDFC"))
            suffix = "@fihdfc";
        else if (bank.contains("ICICI"))
            suffix = "@icici";
        else if (bank.contains("SBI"))
            suffix = "@oksbi";
        else if (bank.contains("AXIS"))
            suffix = "@axisbank";

        return prefix.toLowerCase() + suffix;
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<BankDetails> getByAccountNumber(@PathVariable String accountNumber) {
        BankDetails details = service.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{id}/upi")
    public ResponseEntity<AccountSetupResponse> setupUpi(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String upiId = body.get("upiId");
        BankDetails updated = service.setupUpi(id, upiId);
        return ResponseEntity
                .ok(new AccountSetupResponse(updated.getId(), updated.getAccountNumber(), updated.getUpiId()));
    }

    @GetMapping("/user/{userName}")
    public ResponseEntity<BankDetails> getByUserName(@PathVariable String userName) {
        BankDetails details = service.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("Bank details not found for user: " + userName));
        return ResponseEntity.ok(details);
    }

    @PutMapping("/user/{userName}")
    public ResponseEntity<BankDetails> update(@PathVariable String userName, @RequestBody AccountSetupRequest request) {
        BankDetails existing = service.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("Bank details not found for user: " + userName));

        existing.setBankName(request.getBankName());
        existing.setIfscCode(request.getIfscCode());
        existing.setBranchName(request.getBranchName());
        existing.setAddress(request.getAddress());
        existing.setEmail(request.getEmail());
        existing.setPhoneNumber(request.getPhoneNumber());
        existing.setCreditCardNumber(request.getCreditCardNumber());
        existing.setCvv(request.getCvv());

        BankDetails saved = service.save(existing);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String contact = body.get("contact");
        // generate simple 6-digit OTP
        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        otpStore.put(contact, otp);

        // Send real email if contact looks like an email address
        if (contact != null && contact.contains("@")) {
            try {
                emailService.sendOtpEmail(contact, otp);
            } catch (Exception e) {
                // Log the error but don't fail the request (demo mode)
                System.err.println("Failed to send email: " + e.getMessage());
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("sent", true);
        resp.put("contact", contact);
        // In real app do not return otp; returned here for demo/testing
        resp.put("otp", otp);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String contact = body.get("contact");
        String otp = body.get("otp");
        boolean ok = otp != null && otp.equals(otpStore.get(contact));
        Map<String, Object> resp = new HashMap<>();
        resp.put("verified", ok);
        if (ok)
            otpStore.remove(contact);
        return ResponseEntity.ok(resp);
    }
}
