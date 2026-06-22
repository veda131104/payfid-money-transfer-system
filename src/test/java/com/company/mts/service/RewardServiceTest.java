package com.company.mts.service;

import com.company.mts.dto.RewardLedgerDTO;
import com.company.mts.dto.RewardSummaryDTO;
import com.company.mts.entity.*;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.RewardAccountRepository;
import com.company.mts.repository.RewardLedgerRepository;
import com.company.mts.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardLedgerRepository rewardLedgerRepository;

    @Mock
    private RewardAccountRepository rewardAccountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private RewardService rewardService;

    private TransactionLog testTx;
    private RewardLedger testLedger;
    private RewardAccount testAccount;

    @BeforeEach
    void setUp() {
        testTx = TransactionLog.builder()
                .id(1L)
                .fromAccountId(10L)
                .toAccountId(20L)
                .amount(new BigDecimal("250.00"))
                .status(TransactionStatus.SUCCESS)
                .type(TransactionType.TRANSFER)
                .build();

        testLedger = RewardLedger.builder()
                .accountId(10L)
                .transactionId(1L)
                .transactionAmount(new BigDecimal("250.00"))
                .pointsAwarded(2)
                .description("Reward for transfer of 250.00 — 2 point(s) awarded")
                .grantedAt(LocalDateTime.now())
                .build();
        testLedger.setId(100L);

        testAccount = new RewardAccount(10L);
        testAccount.setId(200L);
    }

    @Test
    void evaluateAndGrantReward_TransactionNotFound_ThrowsException() {
        when(transactionLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rewardService.evaluateAndGrantReward(1L));
        verify(rewardLedgerRepository, never()).save(any());
    }

    @Test
    void evaluateAndGrantReward_AlreadyRewarded_ReturnsExisting() {
        when(transactionLogRepository.findById(1L)).thenReturn(Optional.of(testTx));
        when(rewardLedgerRepository.existsByTransactionId(1L)).thenReturn(true);
        when(rewardLedgerRepository.findByTransactionId(1L)).thenReturn(Optional.of(testLedger));

        RewardLedgerDTO result = rewardService.evaluateAndGrantReward(1L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(new BigDecimal("250.00"), result.getTransactionAmount());
        verify(rewardLedgerRepository, never()).save(any());
    }

    @Test
    void evaluateAndGrantReward_NotEligible_ReturnsNull() {
        testTx.setStatus(TransactionStatus.FAILED);
        when(transactionLogRepository.findById(1L)).thenReturn(Optional.of(testTx));
        when(rewardLedgerRepository.existsByTransactionId(1L)).thenReturn(false);

        RewardLedgerDTO result = rewardService.evaluateAndGrantReward(1L);

        assertNull(result);
        verify(rewardLedgerRepository, never()).save(any());
    }

    @Test
    void evaluateAndGrantReward_Eligible_NewRewardAccount_Success() {
        when(transactionLogRepository.findById(1L)).thenReturn(Optional.of(testTx));
        when(rewardLedgerRepository.existsByTransactionId(1L)).thenReturn(false);
        when(rewardLedgerRepository.save(any(RewardLedger.class))).thenReturn(testLedger);
        when(rewardAccountRepository.findByAccountId(10L)).thenReturn(Optional.empty());
        when(rewardAccountRepository.save(any(RewardAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardLedgerDTO result = rewardService.evaluateAndGrantReward(1L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(2, result.getPointsAwarded());
        verify(rewardLedgerRepository, times(1)).save(any(RewardLedger.class));
        verify(rewardAccountRepository, times(1)).findByAccountId(10L);
        verify(rewardAccountRepository, times(2)).save(any(RewardAccount.class)); // 1 for new RewardAccount, 1 for points update
    }

    @Test
    void evaluateAndGrantReward_Eligible_ExistingRewardAccount_Success() {
        when(transactionLogRepository.findById(1L)).thenReturn(Optional.of(testTx));
        when(rewardLedgerRepository.existsByTransactionId(1L)).thenReturn(false);
        when(rewardLedgerRepository.save(any(RewardLedger.class))).thenReturn(testLedger);
        when(rewardAccountRepository.findByAccountId(10L)).thenReturn(Optional.of(testAccount));
        when(rewardAccountRepository.save(any(RewardAccount.class))).thenReturn(testAccount);

        RewardLedgerDTO result = rewardService.evaluateAndGrantReward(1L);

        assertNotNull(result);
        assertEquals(2, testAccount.getTotalPoints());
        verify(rewardAccountRepository, times(1)).save(testAccount);
    }

    @Test
    void getRewardSummary_Found_ReturnsSummary() {
        testAccount.addPoints(50);
        when(rewardAccountRepository.findByAccountId(10L)).thenReturn(Optional.of(testAccount));

        RewardSummaryDTO result = rewardService.getRewardSummary(10L);

        assertNotNull(result);
        assertEquals(200L, result.getRewardAccountId());
        assertEquals(50, result.getTotalPoints());
    }

    @Test
    void getRewardSummary_NotFound_ThrowsException() {
        when(rewardAccountRepository.findByAccountId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> rewardService.getRewardSummary(999L));
    }

    @Test
    void getRewardHistory_ReturnsList() {
        when(rewardLedgerRepository.findByAccountIdOrderByGrantedAtDesc(10L))
                .thenReturn(Collections.singletonList(testLedger));

        List<RewardLedgerDTO> result = rewardService.getRewardHistory(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    void getOrCreateRewardAccount_Exists_ReturnsSummary() {
        when(rewardAccountRepository.findByAccountId(10L)).thenReturn(Optional.of(testAccount));

        RewardSummaryDTO result = rewardService.getOrCreateRewardAccount(10L);

        assertNotNull(result);
        assertEquals(200L, result.getRewardAccountId());
        verify(rewardAccountRepository, never()).save(any());
    }

    @Test
    void getOrCreateRewardAccount_DoesNotExist_CreatesAndReturnsSummary() {
        when(rewardAccountRepository.findByAccountId(10L)).thenReturn(Optional.empty());
        when(rewardAccountRepository.save(any(RewardAccount.class))).thenReturn(testAccount);

        RewardSummaryDTO result = rewardService.getOrCreateRewardAccount(10L);

        assertNotNull(result);
        assertEquals(200L, result.getRewardAccountId());
        verify(rewardAccountRepository, times(1)).save(any(RewardAccount.class));
    }

    @Test
    void isEligible_Tests() {
        // Valid case
        assertTrue(rewardService.isEligible(testTx));

        // Invalid Status
        testTx.setStatus(TransactionStatus.PENDING);
        assertFalse(rewardService.isEligible(testTx));
        testTx.setStatus(TransactionStatus.SUCCESS);

        // Null Amount
        testTx.setAmount(null);
        assertFalse(rewardService.isEligible(testTx));

        // Under Threshold
        testTx.setAmount(new BigDecimal("100.00"));
        assertFalse(rewardService.isEligible(testTx));
        testTx.setAmount(new BigDecimal("100.01"));
        assertTrue(rewardService.isEligible(testTx));

        // Null account IDs
        testTx.setFromAccountId(null);
        assertFalse(rewardService.isEligible(testTx));
        testTx.setFromAccountId(10L);
        testTx.setToAccountId(null);
        assertFalse(rewardService.isEligible(testTx));
        testTx.setToAccountId(20L);

        // Self-transfer
        testTx.setToAccountId(10L);
        assertFalse(rewardService.isEligible(testTx));
        testTx.setToAccountId(20L);

        // Invalid Type
        testTx.setType(TransactionType.DEBIT);
        assertFalse(rewardService.isEligible(testTx));
    }
}
