package com.company.mts.controller;

import com.company.mts.dto.RewardLedgerDTO;
import com.company.mts.dto.RewardSummaryDTO;
import com.company.mts.service.RewardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing reward endpoints under /api/v1/rewards
 *
 * Public endpoints (no auth required for demo convenience):
 *   GET  /api/v1/rewards/{accountId}/summary          - total points for an account
 *   GET  /api/v1/rewards/{accountId}/history          - full ledger history
 *   POST /api/v1/rewards/evaluate/{transactionId}     - manually trigger evaluation
 *   POST /api/v1/rewards/{accountId}/initialize       - create reward wallet
 */
@RestController
@RequestMapping("/api/v1/rewards")
@CrossOrigin(origins = "*")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    /**
     * GET /api/v1/rewards/{accountId}/summary
     * Returns total accumulated reward points for the account.
     */
    @GetMapping("/{accountId}/summary")
    public ResponseEntity<RewardSummaryDTO> getRewardSummary(@PathVariable Long accountId) {
        RewardSummaryDTO summary = rewardService.getRewardSummary(accountId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/v1/rewards/{accountId}/history
     * Returns the full reward ledger for the account (most recent first).
     */
    @GetMapping("/{accountId}/history")
    public ResponseEntity<List<RewardLedgerDTO>> getRewardHistory(@PathVariable Long accountId) {
        List<RewardLedgerDTO> history = rewardService.getRewardHistory(accountId);
        return ResponseEntity.ok(history);
    }

    /**
     * POST /api/v1/rewards/evaluate/{transactionId}
     * Manually evaluate reward eligibility for a specific transaction.
     * Useful for retries or admin overrides.
     * Returns the reward entry if eligible, or a 204 No Content if not eligible.
     */
    @PostMapping("/evaluate/{transactionId}")
    public ResponseEntity<?> evaluateReward(@PathVariable Long transactionId) {
        RewardLedgerDTO result = rewardService.evaluateAndGrantReward(transactionId);
        if (result == null) {
            return ResponseEntity.noContent().build();   // 204 — not eligible
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/rewards/{accountId}/initialize
     * Creates a reward wallet for the account if one doesn't already exist.
     * Returns the current summary.
     */
    @PostMapping("/{accountId}/initialize")
    public ResponseEntity<RewardSummaryDTO> initializeRewardAccount(@PathVariable Long accountId) {
        RewardSummaryDTO summary = rewardService.getOrCreateRewardAccount(accountId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/v1/rewards/health
     * Simple health-check for the reward module.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "RewardModule",
                "status", "UP",
                "version", "1.0.0"
        ));
    }
}
