package com.company.mts.service;

import com.company.mts.dto.RewardLedgerDTO;
import com.company.mts.dto.RewardSummaryDTO;
import com.company.mts.entity.RewardAccount;
import com.company.mts.entity.RewardLedger;
import com.company.mts.entity.TransactionLog;
import com.company.mts.entity.TransactionStatus;
import com.company.mts.entity.TransactionType;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.RewardAccountRepository;
import com.company.mts.repository.RewardLedgerRepository;
import com.company.mts.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RewardService — orchestrates all reward logic.
 *
 * Eligibility rules (ALL must be true):
 *   1. Transaction status == SUCCESS
 *   2. Transaction amount > 100
 *   3. fromAccountId != toAccountId  (no self-transfer)
 *   4. Transaction type == TRANSFER
 *
 * Calculation: floor(amount / 100) points per eligible transaction.
 */
@Service
public class RewardService {

    private static final Logger logger = LoggerFactory.getLogger(RewardService.class);

    private static final BigDecimal REWARD_THRESHOLD = new BigDecimal("100");
    private static final BigDecimal POINTS_DIVISOR   = new BigDecimal("100");

    private final RewardLedgerRepository  rewardLedgerRepository;
    private final RewardAccountRepository rewardAccountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public RewardService(RewardLedgerRepository rewardLedgerRepository,
                         RewardAccountRepository rewardAccountRepository,
                         TransactionLogRepository transactionLogRepository) {
        this.rewardLedgerRepository  = rewardLedgerRepository;
        this.rewardAccountRepository = rewardAccountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    // =========================================================================
    //  Core: evaluate and grant reward for a completed transaction
    // =========================================================================

    /**
     * Called after a transfer completes. Evaluates eligibility and, if eligible,
     * persists a RewardLedger entry and updates the sender's RewardAccount.
     *
     * @param transactionId  ID of the just-completed TransactionLog
     * @return RewardLedgerDTO if reward was granted, or null if not eligible
     */
    @Transactional
    public RewardLedgerDTO evaluateAndGrantReward(Long transactionId) {
        TransactionLog tx = transactionLogRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with ID: " + transactionId));

        // ---- Guard: already rewarded? ----
        if (rewardLedgerRepository.existsByTransactionId(transactionId)) {
            logger.info("Transaction {} already has a reward entry. Skipping.", transactionId);
            return rewardLedgerRepository.findByTransactionId(transactionId)
                    .map(this::toDTO).orElse(null);
        }

        // ---- Eligibility Check ----
        if (!isEligible(tx)) {
            logger.info("Transaction {} is not eligible for rewards. Status={}, Amount={}, Type={}",
                    transactionId, tx.getStatus(), tx.getAmount(), tx.getType());
            return null;
        }

        // ---- Calculate Points ----
        int points = tx.getAmount().divideToIntegralValue(POINTS_DIVISOR).intValue();

        logger.info("Granting {} reward points for transaction {} (amount={})",
                points, transactionId, tx.getAmount());

        // ---- Persist Ledger Entry ----
        RewardLedger ledger = RewardLedger.builder()
                .accountId(tx.getFromAccountId())
                .transactionId(transactionId)
                .transactionAmount(tx.getAmount())
                .pointsAwarded(points)
                .description(String.format(
                        "Reward for transfer of %.2f — %d point(s) awarded",
                        tx.getAmount(), points))
                .grantedAt(LocalDateTime.now())
                .build();

        RewardLedger saved = rewardLedgerRepository.save(ledger);

        // ---- Update Running Total ----
        RewardAccount rewardAccount = rewardAccountRepository
                .findByAccountId(tx.getFromAccountId())
                .orElseGet(() -> rewardAccountRepository.save(
                        new RewardAccount(tx.getFromAccountId())));

        rewardAccount.addPoints(points);
        rewardAccountRepository.save(rewardAccount);

        logger.info("Reward granted — Account {} now has {} total points",
                tx.getFromAccountId(), rewardAccount.getTotalPoints());

        return toDTO(saved);
    }

    // =========================================================================
    //  Query Methods
    // =========================================================================

    /**
     * Fetch reward summary (total points) for an account.
     */
    public RewardSummaryDTO getRewardSummary(Long accountId) {
        RewardAccount ra = rewardAccountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No reward account found for account ID: " + accountId));
        return toSummaryDTO(ra);
    }

    /**
     * Fetch full reward history for an account (most recent first).
     */
    public List<RewardLedgerDTO> getRewardHistory(Long accountId) {
        return rewardLedgerRepository
                .findByAccountIdOrderByGrantedAtDesc(accountId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get or initialise a RewardAccount for the given account.
     * Useful to call on first login / account creation.
     */
    @Transactional
    public RewardSummaryDTO getOrCreateRewardAccount(Long accountId) {
        RewardAccount ra = rewardAccountRepository.findByAccountId(accountId)
                .orElseGet(() -> rewardAccountRepository.save(new RewardAccount(accountId)));
        return toSummaryDTO(ra);
    }

    // =========================================================================
    //  Eligibility Logic
    // =========================================================================

    /**
     * All four conditions must pass:
     *  1. status == SUCCESS
     *  2. amount > 100
     *  3. fromAccountId != toAccountId  (not self-transfer)
     *  4. type == TRANSFER
     */
    public boolean isEligible(TransactionLog tx) {
        if (tx.getStatus() != TransactionStatus.SUCCESS) {
            logger.info("Transaction {} is not eligible: status is not SUCCESS (status={})", tx.getId(), tx.getStatus());
            return false;
        }
        if (tx.getAmount() == null || tx.getAmount().compareTo(REWARD_THRESHOLD) <= 0) {
            logger.info("Transaction {} is not eligible: amount is null or <= 100 (amount={})", tx.getId(), tx.getAmount());
            return false;
        }
        if (tx.getFromAccountId() == null || tx.getToAccountId() == null) {
            logger.info("Transaction {} is not eligible: fromAccountId or toAccountId is null (from={}, to={})", tx.getId(), tx.getFromAccountId(), tx.getToAccountId());
            return false;
        }
        if (tx.getFromAccountId().equals(tx.getToAccountId())) {
            if (tx.getDescription() == null || !tx.getDescription().toLowerCase().contains("external")) {
                logger.info("Transaction {} is not eligible: standard self-transfer (from==to, desc={})", tx.getId(), tx.getDescription());
                return false;   // standard self-transfer
            }
        }
        if (tx.getType() != TransactionType.TRANSFER) {
            logger.info("Transaction {} is not eligible: type is not TRANSFER (type={})", tx.getId(), tx.getType());
            return false;   // only pure transfers earn points
        }
        return true;
    }

    // =========================================================================
    //  Mappers
    // =========================================================================

    private RewardLedgerDTO toDTO(RewardLedger rl) {
        RewardLedgerDTO dto = new RewardLedgerDTO();
        dto.setId(rl.getId());
        dto.setAccountId(rl.getAccountId());
        dto.setTransactionId(rl.getTransactionId());
        dto.setTransactionAmount(rl.getTransactionAmount());
        dto.setPointsAwarded(rl.getPointsAwarded());
        dto.setDescription(rl.getDescription());
        dto.setGrantedAt(rl.getGrantedAt());
        return dto;
    }

    private RewardSummaryDTO toSummaryDTO(RewardAccount ra) {
        RewardSummaryDTO dto = new RewardSummaryDTO();
        dto.setRewardAccountId(ra.getId());
        dto.setAccountId(ra.getAccountId());
        dto.setTotalPoints(ra.getTotalPoints());
        dto.setCreatedAt(ra.getCreatedAt());
        dto.setUpdatedAt(ra.getUpdatedAt());
        return dto;
    }

    @Transactional
    public RewardLedgerDTO redeemRewards(Long accountId, int pointsToRedeem, String description) {
        RewardAccount rewardAccount = rewardAccountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("No reward account found for account ID: " + accountId));

        if (rewardAccount.getTotalPoints() < pointsToRedeem) {
            throw new IllegalArgumentException("Insufficient reward points. Available: " + rewardAccount.getTotalPoints() + ", Required: " + pointsToRedeem);
        }

        // Deduct points
        rewardAccount.setTotalPoints(rewardAccount.getTotalPoints() - pointsToRedeem);
        rewardAccountRepository.save(rewardAccount);

        // Generate a unique dummy transaction ID using a negative nanoTime
        long dummyTransactionId = -System.nanoTime();

        // Persist ledger entry
        RewardLedger ledger = RewardLedger.builder()
                .accountId(accountId)
                .transactionId(dummyTransactionId)
                .transactionAmount(BigDecimal.ZERO)
                .pointsAwarded(-pointsToRedeem)
                .description(description)
                .grantedAt(LocalDateTime.now())
                .build();

        RewardLedger saved = rewardLedgerRepository.save(ledger);

        logger.info("Reward points redeemed — Account {} now has {} total points. Redeemed: {} points.",
                accountId, rewardAccount.getTotalPoints(), pointsToRedeem);

        return toDTO(saved);
    }
}
