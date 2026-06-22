package com.company.mts.service;

import com.company.mts.dto.TransactionDTO;
import com.company.mts.entity.*;
import com.company.mts.exception.DuplicateTransactionException;
import com.company.mts.exception.InsufficientBalanceException;
import com.company.mts.exception.ResourceNotFoundException;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.BankDetailsRepository;
import com.company.mts.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

        @Mock
        private TransactionLogRepository transactionLogRepository;

        @Mock
        private AccountRepository accountRepository;

        @Mock
        private BankDetailsRepository bankDetailsRepository;

        @Mock
        private RewardService rewardService;

        @InjectMocks
        private TransactionService transactionService;

        private Account fromAccount;
        private Account toAccount;
        private TransactionLog existingTransaction;

        @BeforeEach
        void setUp() {
                fromAccount = Account.builder()
                                .id(1L)
                                .accountNumber("123456789012")
                                .holderName("John Doe")
                                .balance(new BigDecimal("1000.00"))
                                .status(AccountStatus.ACTIVE)
                                .build();

                toAccount = Account.builder()
                                .id(2L)
                                .accountNumber("234567890123")
                                .holderName("Jane Smith")
                                .balance(new BigDecimal("500.00"))
                                .status(AccountStatus.ACTIVE)
                                .build();

                existingTransaction = TransactionLog.builder()
                                .id(1L)
                                .fromAccountId(1L)
                                .toAccountId(2L)
                                .amount(new BigDecimal("300.00"))
                                .type(TransactionType.TRANSFER)
                                .status(TransactionStatus.SUCCESS)
                                .idempotencyKey("TEST-KEY-001")
                                .build();
        }

        @Test
        void executeIdempotentTransfer_Success() {
                String idempotencyKey = "TEST-KEY-002";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));

                TransactionDTO result = transactionService.executeIdempotentTransfer(
                                1L, 2L, new BigDecimal("300.00"), idempotencyKey, "Test transfer");

                assertNotNull(result);
                assertEquals(new BigDecimal("700.00"), fromAccount.getBalance());
                assertEquals(new BigDecimal("800.00"), toAccount.getBalance());
        }

        @Test
        void executeIdempotentTransfer_DuplicateKey_ThrowsException() {
                String idempotencyKey = "TEST-KEY-001";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);
                when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                                .thenReturn(Optional.of(existingTransaction));

                assertThrows(DuplicateTransactionException.class, () -> transactionService.executeIdempotentTransfer(1L,
                                2L, new BigDecimal("300.00"), idempotencyKey, "Test"));
        }

        @Test
        void executeTransferByAccountNumber_Internal_Success() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findByAccountNumber("234567890123")).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));

                TransactionDTO result = transactionService.executeTransferByAccountNumber(
                                "123456789012", "234567890123", new BigDecimal("100.00"), "Internal test");

                assertNotNull(result);
                assertEquals(new BigDecimal("900.00"), fromAccount.getBalance());
        }

        @Test
        void executeTransferByAccountNumber_External_Success() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findByAccountNumber("987654321098")).thenReturn(Optional.empty());
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));

                TransactionDTO result = transactionService.executeTransferByAccountNumber(
                                "123456789012", "987654321098", new BigDecimal("100.00"), "External test");

                assertNotNull(result);
                assertEquals(TransactionType.DEBIT, result.getType());
        }

        @Test
        void logCreditTransaction_Success() {
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));
                TransactionLog result = transactionService.logCreditTransaction(fromAccount, new BigDecimal("500.00"),
                                "Deposit");
                assertNotNull(result);
                assertEquals(TransactionType.CREDIT, result.getType());
        }

        @Test
        void getAccountTransactionHistory_Success() {
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(transactionLogRepository.findByAccountId(1L)).thenReturn(List.of(existingTransaction));

                List<TransactionDTO> results = transactionService.getAccountTransactionHistory(1L);
                assertEquals(1, results.size());
        }

        @Test
        void getTransactionById_Success() {
                when(transactionLogRepository.findById(1L)).thenReturn(Optional.of(existingTransaction));
                TransactionDTO result = transactionService.getTransactionById(1L);
                assertNotNull(result);
                assertEquals(1L, result.getId());
        }

        @Test
        void executeIdempotentTransfer_DuplicateKeyButNotFound_ThrowsException() {
                String idempotencyKey = "TEST-KEY-001";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);
                when(transactionLogRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

                assertThrows(IllegalStateException.class, () -> transactionService.executeIdempotentTransfer(1L,
                                2L, new BigDecimal("300.00"), idempotencyKey, "Test"));
        }

        @Test
        void executeIdempotentTransfer_SelfTransfer_ThrowsException() {
                assertThrows(IllegalArgumentException.class, () -> transactionService.executeIdempotentTransfer(1L,
                                1L, new BigDecimal("300.00"), "KEY", "Test"));
        }

        @Test
        void executeIdempotentTransfer_SenderNotFound_ThrowsException() {
                when(transactionLogRepository.existsByIdempotencyKey("KEY")).thenReturn(false);
                when(accountRepository.findById(1L)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> transactionService.executeIdempotentTransfer(1L,
                                2L, new BigDecimal("300.00"), "KEY", "Test"));
        }

        @Test
        void executeIdempotentTransfer_ReceiverNotFound_ThrowsException() {
                when(transactionLogRepository.existsByIdempotencyKey("KEY")).thenReturn(false);
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> transactionService.executeIdempotentTransfer(1L,
                                2L, new BigDecimal("300.00"), "KEY", "Test"));
        }

        @Test
        void executeIdempotentTransfer_TransactionException_SavesFailedStatus() {
                String idempotencyKey = "TEST-KEY-EX";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

                // Force InsufficientBalanceException by transferring too much
                BigDecimal highAmount = new BigDecimal("2000.00");
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));

                assertThrows(InsufficientBalanceException.class, () -> transactionService.executeIdempotentTransfer(
                                1L, 2L, highAmount, idempotencyKey, "High amount"));

                verify(transactionLogRepository, times(1)).save(argThat(tx -> 
                        tx.getStatus() == TransactionStatus.FAILED && 
                        tx.getFailureReason().contains("Insufficient balance")
                ));
        }

        @Test
        void executeIdempotentTransfer_DescriptionNull_GeneratesDefaultDescription() {
                String idempotencyKey = "TEST-KEY-DESC";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> {
                        TransactionLog tx = i.getArgument(0);
                        tx.setId(100L);
                        return tx;
                });

                TransactionDTO result = transactionService.executeIdempotentTransfer(
                                1L, 2L, new BigDecimal("100.00"), idempotencyKey, null);

                assertNotNull(result);
                assertEquals("Transfer from account 1 to 2", result.getDescription());
        }

        @Test
        void executeIdempotentTransfer_RewardServiceThrows_DoesNotCrashTransaction() {
                String idempotencyKey = "TEST-KEY-REWARD-EX";
                when(transactionLogRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> {
                        TransactionLog tx = i.getArgument(0);
                        tx.setId(100L);
                        return tx;
                });
                doThrow(new RuntimeException("Reward error")).when(rewardService).evaluateAndGrantReward(anyLong());

                TransactionDTO result = transactionService.executeIdempotentTransfer(
                                1L, 2L, new BigDecimal("100.00"), idempotencyKey, "desc");

                assertNotNull(result);
                assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        }

        @Test
        void executeTransferByAccountNumber_SelfTransfer_ThrowsException() {
                assertThrows(IllegalArgumentException.class, () -> transactionService.executeTransferByAccountNumber(
                                "123456789012", "123456789012", BigDecimal.TEN, "desc"));
        }

        @Test
        void executeTransferByAccountNumber_ProvisionSender_Success() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.empty());
                BankDetails bd = BankDetails.builder().accountNumber("123456789012").userName("John Doe").build();
                when(bankDetailsRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(bd));
                when(accountRepository.save(any(Account.class))).thenReturn(fromAccount);
                when(accountRepository.findByAccountNumber("234567890123")).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> {
                        TransactionLog tx = i.getArgument(0);
                        tx.setId(100L);
                        return tx;
                });

                TransactionDTO result = transactionService.executeTransferByAccountNumber(
                                "123456789012", "234567890123", new BigDecimal("100.00"), "desc");

                assertNotNull(result);
                assertEquals(100L, result.getId());
        }

        @Test
        void executeTransferByAccountNumber_ProvisionSenderNotFound_ThrowsException() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.empty());
                when(bankDetailsRepository.findByAccountNumber("123456789012")).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, () -> transactionService.executeTransferByAccountNumber(
                                "123456789012", "234567890123", BigDecimal.TEN, "desc"));
        }

        @Test
        void executeTransferByAccountNumber_ReceiverNotFound_UpiNot12Digits_ThrowsException() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findByAccountNumber("abc")).thenReturn(Optional.empty());

                assertThrows(IllegalArgumentException.class, () -> transactionService.executeTransferByAccountNumber(
                                "123456789012", "abc", BigDecimal.TEN, "desc"));
        }

        @Test
        void executeTransferByAccountNumber_ReceiverNotFound_NullUpi_ThrowsException() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findByAccountNumber(null)).thenReturn(Optional.empty());

                assertThrows(IllegalArgumentException.class, () -> transactionService.executeTransferByAccountNumber(
                                "123456789012", null, BigDecimal.TEN, "desc"));
        }

        @Test
        void executeTransferByAccountNumber_Internal_RewardServiceThrows_DoesNotCrashTransaction() {
                when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findByAccountNumber("234567890123")).thenReturn(Optional.of(toAccount));
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> {
                        TransactionLog tx = i.getArgument(0);
                        tx.setId(100L);
                        return tx;
                });
                doThrow(new RuntimeException("Reward error")).when(rewardService).evaluateAndGrantReward(anyLong());

                TransactionDTO result = transactionService.executeTransferByAccountNumber(
                                "123456789012", "234567890123", new BigDecimal("100.00"), "desc");

                assertNotNull(result);
                assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        }

        @Test
        void logDebitTransaction_Success() {
                when(transactionLogRepository.save(any(TransactionLog.class))).thenAnswer(i -> i.getArgument(0));
                TransactionLog result = transactionService.logDebitTransaction(fromAccount, new BigDecimal("500.00"),
                                "Withdrawal");
                assertNotNull(result);
                assertEquals(TransactionType.DEBIT, result.getType());
        }

        @Test
        void getAllTransactions_Success() {
                when(transactionLogRepository.findAll()).thenReturn(List.of(existingTransaction));
                List<TransactionDTO> result = transactionService.getAllTransactions();
                assertEquals(1, result.size());
        }

        @Test
        void getFailedTransactions_Success() {
                when(transactionLogRepository.findFailedTransactionsByAccountId(1L)).thenReturn(List.of(existingTransaction));
                List<TransactionDTO> result = transactionService.getFailedTransactions(1L);
                assertEquals(1, result.size());
        }

        @Test
        void getAccountTransactionHistory_AccountNotFound_ThrowsException() {
                when(accountRepository.findById(999L)).thenReturn(Optional.empty());
                assertThrows(ResourceNotFoundException.class, () -> transactionService.getAccountTransactionHistory(999L));
        }

        @Test
        void getTransactionById_NotFound_ThrowsException() {
                when(transactionLogRepository.findById(999L)).thenReturn(Optional.empty());
                assertThrows(ResourceNotFoundException.class, () -> transactionService.getTransactionById(999L));
        }

        @Test
        void convertToDTO_FetchFromAndToAccounts_Success() {
                TransactionLog tx = TransactionLog.builder()
                                .id(1L)
                                .fromAccountId(1L)
                                .toAccountId(2L)
                                .amount(BigDecimal.TEN)
                                .build();
                when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

                when(transactionLogRepository.findById(1L)).thenReturn(Optional.of(tx));

                TransactionDTO result = transactionService.getTransactionById(1L);

                assertNotNull(result);
                assertEquals("123456789012", result.getFromAccountNumber());
                assertEquals("234567890123", result.getToAccountNumber());
        }
}