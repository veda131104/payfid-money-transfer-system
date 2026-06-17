package com.company.mts.controller;

import com.company.mts.repository.AuthUserRepository;
import com.company.mts.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@lombok.extern.slf4j.Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AuthUserRepository authUserRepository;
    private final com.company.mts.repository.AccountRepository accountRepository;

    public AnalyticsController(AnalyticsService analyticsService,
            AuthUserRepository authUserRepository,
            com.company.mts.repository.AccountRepository accountRepository) {
        this.analyticsService = analyticsService;
        this.authUserRepository = authUserRepository;
        this.accountRepository = accountRepository;
    }

    private UserContext resolveContext(String name) {
        log.info("Resolving analytics context for name: [{}]", name);
        if (name == null || name.isBlank() || name.equals("User") || name.equals("undefined")) {
            log.warn("Invalid or default name [{}], falling back to default admin context (Veda)", name);
            return new UserContext("veda131104", "ADMIN", "Veda Jagannath");
        }

        return authUserRepository.findByNameIgnoreCase(name)
                .map(user -> {
                    log.info("Found AuthUser: {} with role: {}", user.getName(), user.getRole());
                    String holderName = accountRepository.findAll().stream()
                            .filter(acc -> acc.getHolderName() != null &&
                                    (acc.getHolderName().equalsIgnoreCase(user.getName()) ||
                                            user.getName().toLowerCase().contains(acc.getHolderName().toLowerCase()) ||
                                            acc.getHolderName().toLowerCase().contains(user.getName().toLowerCase())))
                            .findFirst()
                            .map(acc -> acc.getHolderName())
                            .orElse(user.getName());
                    log.info("Resolved holderName: {} for analytics filtering", holderName);
                    return new UserContext(user.getName(), user.getRole(), holderName);
                })
                .orElseGet(() -> {
                    log.warn("No AuthUser found for name: [{}], falling back to default admin context", name);
                    return new UserContext("veda131104", "ADMIN", "Veda Jagannath");
                });
    }

    private static class UserContext {
        final String name;
        final String role;
        final String holderName;

        UserContext(String n, String r, String h) {
            name = n;
            role = r;
            holderName = h;
        }
    }

    @GetMapping("/transaction-volume")
    public ResponseEntity<List<Map<String, Object>>> getTransactionVolume(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String name) {
        UserContext ctx = resolveContext(name);
        return ResponseEntity.ok(analyticsService.getTransactionVolume(ctx.name, ctx.role, ctx.holderName));
    }

    @GetMapping("/account-activity")
    public ResponseEntity<List<Map<String, Object>>> getAccountActivity(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String name) {
        UserContext ctx = resolveContext(name);
        return ResponseEntity.ok(analyticsService.getAccountActivity(ctx.name, ctx.role, ctx.holderName));
    }

    @GetMapping("/success-rate")
    public ResponseEntity<Map<String, Object>> getSuccessRate(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String name) {
        UserContext ctx = resolveContext(name);
        return ResponseEntity.ok(analyticsService.getSuccessRate(ctx.name, ctx.role, ctx.holderName));
    }

    @GetMapping("/peak-hours")
    public ResponseEntity<List<Map<String, Object>>> getPeakHours(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String name) {
        UserContext ctx = resolveContext(name);
        return ResponseEntity.ok(analyticsService.getPeakHours(ctx.name, ctx.role, ctx.holderName));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(analyticsService.checkStatus());
    }
}
