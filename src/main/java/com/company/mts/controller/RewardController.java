package com.company.mts.controller;

import com.company.mts.dto.*;
import com.company.mts.service.RewardService;
import com.company.mts.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rewards")
@CrossOrigin(origins = "*")
public class RewardController {

    private final RewardService rewardService;
    private final VoucherService voucherService;

    public RewardController(RewardService rewardService, VoucherService voucherService) {
        this.rewardService = rewardService;
        this.voucherService = voucherService;
    }

    @GetMapping("/{accountId}/summary")
    public ResponseEntity<RewardSummaryDTO> getRewardSummary(@PathVariable Long accountId) {
        return ResponseEntity.ok(rewardService.getRewardSummary(accountId));
    }

    @GetMapping("/{accountId}/history")
    public ResponseEntity<List<RewardLedgerDTO>> getRewardHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(rewardService.getRewardHistory(accountId));
    }

    @PostMapping("/evaluate/{transactionId}")
    public ResponseEntity<?> evaluateReward(@PathVariable Long transactionId) {
        RewardLedgerDTO result = rewardService.evaluateAndGrantReward(transactionId);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{accountId}/initialize")
    public ResponseEntity<RewardSummaryDTO> initializeRewardAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(rewardService.getOrCreateRewardAccount(accountId));
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<VoucherDTO>> getAvailableVouchers(@RequestParam Long accountId) {
        return ResponseEntity.ok(voucherService.getAvailableVouchers(accountId));
    }

    @PostMapping("/vouchers/redeem")
    public ResponseEntity<?> redeemVoucher(@RequestBody VoucherClaimRequest request) {
        try {
            VoucherRedemptionDTO result = voucherService.redeemVoucher(request);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{accountId}/redemptions")
    public ResponseEntity<List<VoucherRedemptionDTO>> getRedemptionHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(voucherService.getRedemptionHistory(accountId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "RewardModule",
                "status", "UP",
                "version", "1.0.0"
        ));
    }
}
