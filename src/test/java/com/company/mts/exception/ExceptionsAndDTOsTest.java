package com.company.mts.exception;

import com.company.mts.dto.*;
import com.company.mts.entity.*;
import com.company.mts.controller.TransferRequest;
import com.company.mts.controller.AmountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionsAndDTOsTest {

    // =========================================================================
    // Exceptions
    // =========================================================================

    @Test
    void testCustomExceptions() {
        assertNotNull(new DuplicateAccountException("msg").getMessage());
        assertNotNull(new DuplicateUserException("msg").getMessage());
        assertNotNull(new InactiveAccountException("msg").getMessage());
        assertNotNull(new InsufficientBalanceException("msg").getMessage());
        assertNotNull(new InvalidCredentialsException("msg").getMessage());
        assertNotNull(new ResourceNotFoundException("msg").getMessage());

        // DuplicateTransactionException takes (String message, Long existingTransactionId)
        DuplicateTransactionException dte = new DuplicateTransactionException("msg", 100L);
        assertEquals("msg", dte.getMessage());
        assertEquals("TRX-409", dte.getErrorCode());
        assertEquals(100L, dte.getExistingTransactionId());
    }

    // =========================================================================
    // GlobalExceptionHandler
    // =========================================================================

    @Test
    void testGlobalExceptionHandler() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // 1. ResourceNotFoundException → 404
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res1 =
                handler.handleNotFound(new ResourceNotFoundException("Not found"));
        assertEquals(HttpStatus.NOT_FOUND, res1.getStatusCode());
        assertEquals("ACC-404", res1.getBody().getErrorCode());

        // 2. DuplicateAccountException → 409
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res2 =
                handler.handleDuplicateAccount(new DuplicateAccountException("Duplicate acc"));
        assertEquals(HttpStatus.CONFLICT, res2.getStatusCode());
        assertEquals("ACC-409", res2.getBody().getErrorCode());

        // 3. DuplicateUserException → 409
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res3 =
                handler.handleDuplicateUser(new DuplicateUserException("Duplicate user"));
        assertEquals(HttpStatus.CONFLICT, res3.getStatusCode());
        assertEquals("AUTH-409", res3.getBody().getErrorCode());

        // 4. InvalidCredentialsException → 401
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res4 =
                handler.handleInvalidCredentials(new InvalidCredentialsException("Invalid credentials"));
        assertEquals(HttpStatus.UNAUTHORIZED, res4.getStatusCode());
        assertEquals("AUTH-401", res4.getBody().getErrorCode());

        // 5. DuplicateTransactionException → 409
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res5 =
                handler.handleDuplicateTransaction(new DuplicateTransactionException("Duplicate tx", 500L));
        assertEquals(HttpStatus.CONFLICT, res5.getStatusCode());
        assertEquals("TRX-409", res5.getBody().getErrorCode());
        assertEquals("500", res5.getBody().getMetadata().get("existingTransactionId"));

        // 6. InsufficientBalanceException → 400
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res6 =
                handler.handleInsufficientBalance(new InsufficientBalanceException("Insufficient"));
        assertEquals(HttpStatus.BAD_REQUEST, res6.getStatusCode());
        assertEquals("TRX-400-BALANCE", res6.getBody().getErrorCode());

        // 7. InactiveAccountException → 403
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res7 =
                handler.handleInactiveAccount(new InactiveAccountException("Inactive"));
        assertEquals(HttpStatus.FORBIDDEN, res7.getStatusCode());
        assertEquals("ACC-403", res7.getBody().getErrorCode());

        // 8. IllegalStateException → 400
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res8 =
                handler.handleIllegalState(new IllegalStateException("Illegal state"));
        assertEquals(HttpStatus.BAD_REQUEST, res8.getStatusCode());
        assertEquals("SYS-400", res8.getBody().getErrorCode());

        // 9. IllegalArgumentException → 400
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res9 =
                handler.handleIllegalArgument(new IllegalArgumentException("Illegal arg"));
        assertEquals(HttpStatus.BAD_REQUEST, res9.getStatusCode());
        assertEquals("VAL-400", res9.getBody().getErrorCode());

        // 10. Generic Exception → 500
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res10 =
                handler.handleGenericException(new Exception("Generic"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res10.getStatusCode());
        assertEquals("SYS-500", res10.getBody().getErrorCode());

        // 11. MethodArgumentNotValidException → 400
        MethodArgumentNotValidException mockEx = mock(MethodArgumentNotValidException.class);
        BindingResult mockBinding = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "defaultMessage");
        when(mockBinding.getAllErrors()).thenReturn(Collections.singletonList(fieldError));
        when(mockEx.getBindingResult()).thenReturn(mockBinding);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res11 =
                handler.handleValidationExceptions(mockEx);
        assertEquals(HttpStatus.BAD_REQUEST, res11.getStatusCode());
        assertEquals("defaultMessage", res11.getBody().getValidationErrors().get("field"));
    }

    @Test
    void testErrorResponseGettersSetters() {
        LocalDateTime now = LocalDateTime.now();
        GlobalExceptionHandler.ErrorResponse resp =
                new GlobalExceptionHandler.ErrorResponse("CODE", 400, "msg", now);

        resp.setErrorCode("NEWCODE");
        resp.setStatus(404);
        resp.setMessage("newMsg");
        LocalDateTime time = LocalDateTime.now();
        resp.setTimestamp(time);
        Map<String, String> valErrors = new HashMap<>();
        resp.setValidationErrors(valErrors);
        Map<String, String> meta = new HashMap<>();
        resp.setMetadata(meta);

        assertEquals("NEWCODE", resp.getErrorCode());
        assertEquals(404, resp.getStatus());
        assertEquals("newMsg", resp.getMessage());
        assertEquals(time, resp.getTimestamp());
        assertEquals(valErrors, resp.getValidationErrors());
        assertEquals(meta, resp.getMetadata());
    }

    // =========================================================================
    // DTOs
    // =========================================================================

    @Test
    void testDTOs() {
        LocalDateTime now = LocalDateTime.now();

        // AccountBalanceDTO — uses field "accountId" not "id"
        AccountBalanceDTO balDto = new AccountBalanceDTO(1L, "123", "name", BigDecimal.TEN, "ACTIVE");
        assertEquals(1L, balDto.getAccountId());
        assertEquals("123", balDto.getAccountNumber());
        assertEquals("name", balDto.getHolderName());
        assertEquals(BigDecimal.TEN, balDto.getBalance());
        assertEquals("ACTIVE", balDto.getStatus());
        balDto.setAccountId(2L); balDto.setAccountNumber("456");
        balDto.setHolderName("h2"); balDto.setBalance(BigDecimal.ZERO); balDto.setStatus("LOCKED");
        assertEquals(2L, balDto.getAccountId());

        // AccountSetupRequest
        AccountSetupRequest asr = new AccountSetupRequest();
        asr.setAccountNumber("1"); asr.setBankName("b"); asr.setIfscCode("i");
        asr.setBranchName("br"); asr.setAddress("ad"); asr.setEmail("e");
        asr.setPhoneNumber("p"); asr.setUserName("u");
        asr.setCreditCardNumber("c"); asr.setCvv("cv"); asr.setExpiryDate("ex");
        assertEquals("1", asr.getAccountNumber());
        assertEquals("b", asr.getBankName());
        assertEquals("i", asr.getIfscCode());
        assertEquals("br", asr.getBranchName());
        assertEquals("ad", asr.getAddress());
        assertEquals("e", asr.getEmail());
        assertEquals("p", asr.getPhoneNumber());
        assertEquals("u", asr.getUserName());
        assertEquals("c", asr.getCreditCardNumber());
        assertEquals("cv", asr.getCvv());
        assertEquals("ex", asr.getExpiryDate());

        // AccountSetupResponse
        AccountSetupResponse asresp = new AccountSetupResponse(1L, "123", "upi");
        asresp.setId(2L); asresp.setAccountNumber("456"); asresp.setUpiId("upi2");
        assertEquals(2L, asresp.getId());
        assertEquals("456", asresp.getAccountNumber());
        assertEquals("upi2", asresp.getUpiId());

        // AuthResponse — actual constructor: (String name, String rememberToken, boolean firstLogin, String role)
        AuthResponse authResp = new AuthResponse("alice", "remToken", true, "ADMIN");
        assertEquals("alice", authResp.getName());
        assertEquals("remToken", authResp.getRememberToken());
        assertTrue(authResp.isFirstLogin());
        authResp.setName("bob");
        authResp.setEmail("bob@test.com");
        authResp.setRememberToken("newToken");
        authResp.setFirstLogin(false);
        assertEquals("bob", authResp.getName());
        assertEquals("bob@test.com", authResp.getEmail());
        assertEquals("newToken", authResp.getRememberToken());
        assertFalse(authResp.isFirstLogin());

        // CredentialsResponse — actual fields: name, password
        CredentialsResponse credResp = new CredentialsResponse("userName", "pwd");
        assertEquals("userName", credResp.getName());
        assertEquals("pwd", credResp.getPassword());
        credResp.setName("n2"); credResp.setPassword("p2");
        assertEquals("n2", credResp.getName());
        assertEquals("p2", credResp.getPassword());

        // ForgotPasswordRequest — actual field: "name" not "email"
        ForgotPasswordRequest fpr = new ForgotPasswordRequest();
        fpr.setName("alice");
        assertEquals("alice", fpr.getName());

        // IdempotentTransferRequest
        IdempotentTransferRequest itr = new IdempotentTransferRequest();
        itr.setFromAccountId(1L); itr.setToAccountId(2L);
        itr.setAmount(BigDecimal.ONE); itr.setIdempotencyKey("k");
        assertEquals(1L, itr.getFromAccountId());
        assertEquals(2L, itr.getToAccountId());
        assertEquals(BigDecimal.ONE, itr.getAmount());
        assertEquals("k", itr.getIdempotencyKey());

        // LoginRequest
        LoginRequest lr = new LoginRequest();
        lr.setName("n"); lr.setPassword("p");
        assertEquals("n", lr.getName());
        assertEquals("p", lr.getPassword());

        // LoginResponse — uses Lombok @Data; actual fields: userId, email, name, token, tokenType, rememberToken, firstLogin
        LoginResponse lresp = new LoginResponse();
        lresp.setUserId(1L); lresp.setEmail("e@test.com"); lresp.setName("user");
        lresp.setToken("tok"); lresp.setTokenType("Bearer"); lresp.setRememberToken("rt");
        lresp.setFirstLogin(true);
        assertEquals(1L, lresp.getUserId());
        assertEquals("e@test.com", lresp.getEmail());
        assertEquals("user", lresp.getName());
        assertEquals("tok", lresp.getToken());
        assertEquals("Bearer", lresp.getTokenType());
        assertEquals("rt", lresp.getRememberToken());
        assertTrue(lresp.isFirstLogin());

        // ResetPasswordRequest
        ResetPasswordRequest rpr = new ResetPasswordRequest();
        rpr.setToken("t"); rpr.setNewPassword("p");
        assertEquals("t", rpr.getToken());
        assertEquals("p", rpr.getNewPassword());

        // RewardLedgerDTO
        RewardLedgerDTO rld = new RewardLedgerDTO();
        rld.setId(1L); rld.setAccountId(2L); rld.setTransactionId(3L);
        rld.setTransactionAmount(BigDecimal.TEN);
        rld.setPointsAwarded(5); rld.setDescription("d"); rld.setGrantedAt(now);
        assertEquals(1L, rld.getId());
        assertEquals(2L, rld.getAccountId());
        assertEquals(3L, rld.getTransactionId());
        assertEquals(BigDecimal.TEN, rld.getTransactionAmount());
        assertEquals(5, rld.getPointsAwarded());
        assertEquals("d", rld.getDescription());
        assertEquals(now, rld.getGrantedAt());

        // RewardSummaryDTO
        RewardSummaryDTO rsd = new RewardSummaryDTO();
        rsd.setRewardAccountId(1L); rsd.setAccountId(2L); rsd.setTotalPoints(10);
        rsd.setCreatedAt(now); rsd.setUpdatedAt(now);
        assertEquals(1L, rsd.getRewardAccountId());
        assertEquals(2L, rsd.getAccountId());
        assertEquals(10, rsd.getTotalPoints());
        assertEquals(now, rsd.getCreatedAt());
        assertEquals(now, rsd.getUpdatedAt());

        // SignupRequest
        SignupRequest sr = new SignupRequest();
        sr.setName("n"); sr.setEmail("e"); sr.setPassword("p");
        assertEquals("n", sr.getName());
        assertEquals("e", sr.getEmail());
        assertEquals("p", sr.getPassword());

        // TokenRequest
        TokenRequest tr = new TokenRequest();
        tr.setToken("t");
        assertEquals("t", tr.getToken());

        // AuthResponse constructors
        AuthResponse ar1 = new AuthResponse("alice");
        assertEquals("alice", ar1.getName());
        assertFalse(ar1.isFirstLogin());
        assertEquals("USER", ar1.getRole());

        AuthResponse ar2 = new AuthResponse("alice", "token");
        assertEquals("alice", ar2.getName());
        assertEquals("token", ar2.getRememberToken());
        assertFalse(ar2.isFirstLogin());
        assertEquals("USER", ar2.getRole());

        AuthResponse ar3 = new AuthResponse("alice", "token", true);
        assertEquals("alice", ar3.getName());
        assertEquals("token", ar3.getRememberToken());
        assertTrue(ar3.isFirstLogin());
        assertEquals("USER", ar3.getRole());

        // AccountSetupResponse default constructor
        AccountSetupResponse asrDefault = new AccountSetupResponse();
        assertNull(asrDefault.getId());

        // AccountBalanceDTO default constructor
        AccountBalanceDTO abdDefault = new AccountBalanceDTO();
        assertNull(abdDefault.getAccountId());

        // TransferRequest
        TransferRequest trq1 = new TransferRequest();
        trq1.setFromAccountId(1L);
        trq1.setToAccountId(2L);
        trq1.setAmount(BigDecimal.TEN);
        assertEquals(1L, trq1.getFromAccountId());
        assertEquals(2L, trq1.getToAccountId());
        assertEquals(BigDecimal.TEN, trq1.getAmount());

        TransferRequest trq2 = new TransferRequest(3L, 4L, BigDecimal.ONE);
        assertEquals(3L, trq2.getFromAccountId());
        assertEquals(4L, trq2.getToAccountId());
        assertEquals(BigDecimal.ONE, trq2.getAmount());

        // AmountRequest
        AmountRequest amr1 = new AmountRequest();
        amr1.setAmount(BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, amr1.getAmount());

        AmountRequest amr2 = new AmountRequest(BigDecimal.ONE);
        assertEquals(BigDecimal.ONE, amr2.getAmount());

        // TransferByAccountNumberRequest
        TransferByAccountNumberRequest tbanr1 = new TransferByAccountNumberRequest();
        tbanr1.setFromAccountNumber("111");
        tbanr1.setToAccountNumber("222");
        tbanr1.setAmount(BigDecimal.TEN);
        assertEquals("111", tbanr1.getFromAccountNumber());
        assertEquals("222", tbanr1.getToAccountNumber());
        assertEquals(BigDecimal.TEN, tbanr1.getAmount());

        TransferByAccountNumberRequest tbanr2 = new TransferByAccountNumberRequest("333", "444", BigDecimal.ONE);
        assertEquals("333", tbanr2.getFromAccountNumber());
        assertEquals("444", tbanr2.getToAccountNumber());
        assertEquals(BigDecimal.ONE, tbanr2.getAmount());

        // IdempotentTransferRequest additional fields/constructors
        IdempotentTransferRequest itr1 = new IdempotentTransferRequest(10L, 20L, BigDecimal.TEN, "key");
        assertEquals(10L, itr1.getFromAccountId());
        assertEquals(20L, itr1.getToAccountId());
        assertEquals(BigDecimal.TEN, itr1.getAmount());
        assertEquals("key", itr1.getIdempotencyKey());
        itr1.setDescription("desc");
        assertEquals("desc", itr1.getDescription());

        // AuthResponse setRole
        authResp.setRole("USER");
        assertEquals("USER", authResp.getRole());
    }

    // =========================================================================
    // Entities
    // =========================================================================

    @Test
    void testEntities() {
        LocalDateTime now = LocalDateTime.now();

        // ----- Account -----
        Account acc = Account.builder()
                .id(1L).accountNumber("123").holderName("holder")
                .balance(BigDecimal.TEN).status(AccountStatus.ACTIVE)
                .createdAt(now).lastUpdated(now).build();

        assertEquals(1L, acc.getId());
        assertEquals("123", acc.getAccountNumber());
        assertEquals("holder", acc.getHolderName());
        assertEquals(BigDecimal.TEN, acc.getBalance());
        assertEquals(AccountStatus.ACTIVE, acc.getStatus());
        assertEquals(now, acc.getCreatedAt());
        assertEquals(now, acc.getLastUpdated());

        acc.setId(2L); acc.setAccountNumber("456"); acc.setHolderName("h2");
        acc.setBalance(BigDecimal.ZERO); acc.setStatus(AccountStatus.LOCKED);
        assertEquals(2L, acc.getId());
        assertEquals(AccountStatus.LOCKED, acc.getStatus());

        acc.setCreatedAt(null);
        acc.prePersist();
        assertNotNull(acc.getCreatedAt());
        assertNotNull(acc.getLastUpdated());
        acc.preUpdate();

        // ----- AuthUser -----
        AuthUser user = new AuthUser();
        user.setId(1L); user.setName("alice"); user.setEmail("alice@test.com");
        user.setPassword("pw"); user.setRole("ADMIN"); user.setFirstLogin(true);
        // actual token fields: recoveryToken, rememberToken
        user.setRecoveryToken("rt"); user.setRecoveryTokenExpiry(now);
        user.setRememberToken("mt"); user.setRememberTokenExpiry(now);

        assertEquals(1L, user.getId());
        assertEquals("alice", user.getName());
        assertEquals("alice@test.com", user.getEmail());
        assertEquals("pw", user.getPassword());
        assertEquals("ADMIN", user.getRole());
        assertTrue(user.isFirstLogin());
        assertEquals("rt", user.getRecoveryToken());
        assertEquals(now, user.getRecoveryTokenExpiry());
        assertEquals("mt", user.getRememberToken());
        assertEquals(now, user.getRememberTokenExpiry());

        user.setCreatedAt(null);
        user.prePersist();
        assertNotNull(user.getCreatedAt());

        // ----- BankDetails -----
        BankDetails bd = BankDetails.builder()
                .id(1L).accountNumber("123").bankName("bank").ifscCode("ifsc")
                .branchName("branch").address("addr").email("e@test.com")
                .phoneNumber("p").userName("u").creditCardNumber("cc")
                .cvv("cvv").expiryDate("exp").upiId("upi").pin("1234")
                .createdAt(now).lastUpdated(now).build();

        assertEquals(1L, bd.getId());
        assertEquals("123", bd.getAccountNumber());
        assertEquals("bank", bd.getBankName());
        assertEquals("ifsc", bd.getIfscCode());
        assertEquals("branch", bd.getBranchName());
        assertEquals("addr", bd.getAddress());
        assertEquals("e@test.com", bd.getEmail());
        assertEquals("p", bd.getPhoneNumber());
        assertEquals("u", bd.getUserName());
        assertEquals("cc", bd.getCreditCardNumber());
        assertEquals("cvv", bd.getCvv());
        assertEquals("exp", bd.getExpiryDate());
        assertEquals("upi", bd.getUpiId());
        assertEquals("1234", bd.getPin());

        bd.setId(2L); bd.setAccountNumber("456"); bd.setBankName("b2"); bd.setIfscCode("ifsc2");
        bd.setBranchName("br2"); bd.setAddress("ad2"); bd.setEmail("e2"); bd.setPhoneNumber("p2");
        bd.setUserName("u2"); bd.setCreditCardNumber("cc2"); bd.setCvv("cvv2");
        bd.setExpiryDate("exp2"); bd.setUpiId("upi2"); bd.setPin("5678");
        assertEquals(2L, bd.getId());

        bd.setCreatedAt(null);
        bd.prePersist();
        assertNotNull(bd.getCreatedAt());
        assertNotNull(bd.getLastUpdated());
        bd.preUpdate();

        // ----- RewardAccount -----
        RewardAccount ra = new RewardAccount(5L);
        ra.setId(10L); ra.addPoints(50); ra.setCreatedAt(now); ra.setUpdatedAt(now);
        assertEquals(10L, ra.getId());
        assertEquals(5L, ra.getAccountId());
        assertEquals(50, ra.getTotalPoints());
        assertEquals(now, ra.getCreatedAt());
        assertEquals(now, ra.getUpdatedAt());

        ra.setCreatedAt(null);
        ra.prePersist();
        assertNotNull(ra.getCreatedAt());
        assertNotNull(ra.getUpdatedAt());
        ra.preUpdate();

        // ----- TransactionLog — actual fields: transactionDate, failureReason (no errorCode/errorMessage/timestamp) -----
        TransactionLog tl = TransactionLog.builder()
                .id(1L).fromAccountId(2L).toAccountId(3L)
                .amount(BigDecimal.ONE).type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("key").failureReason("reason")
                .transactionDate(now).description("desc")
                .fromAccountBalanceBefore(BigDecimal.ZERO).fromAccountBalanceAfter(BigDecimal.ONE)
                .toAccountBalanceBefore(BigDecimal.ZERO).toAccountBalanceAfter(BigDecimal.ONE)
                .build();

        assertEquals(1L, tl.getId());
        assertEquals(2L, tl.getFromAccountId());
        assertEquals(3L, tl.getToAccountId());
        assertEquals(BigDecimal.ONE, tl.getAmount());
        assertEquals(TransactionType.TRANSFER, tl.getType());
        assertEquals(TransactionStatus.SUCCESS, tl.getStatus());
        assertEquals("key", tl.getIdempotencyKey());
        assertEquals("reason", tl.getFailureReason());
        assertEquals(now, tl.getTransactionDate());
        assertEquals("desc", tl.getDescription());
        assertNotNull(tl.getFromAccountBalanceBefore());
        assertNotNull(tl.getFromAccountBalanceAfter());
        assertNotNull(tl.getToAccountBalanceBefore());
        assertNotNull(tl.getToAccountBalanceAfter());

        tl.setId(2L); tl.setFromAccountId(4L); tl.setToAccountId(5L);
        tl.setAmount(BigDecimal.TEN); tl.setType(TransactionType.CREDIT);
        tl.setStatus(TransactionStatus.FAILED); tl.setIdempotencyKey("k2");
        tl.setFailureReason("fr2"); tl.setDescription("d2");
        tl.setFromAccountBalanceBefore(BigDecimal.ONE); tl.setFromAccountBalanceAfter(BigDecimal.ONE);
        tl.setToAccountBalanceBefore(BigDecimal.ONE); tl.setToAccountBalanceAfter(BigDecimal.ONE);
        assertEquals(2L, tl.getId());
        assertEquals("fr2", tl.getFailureReason());

        tl.setTransactionDate(null);
        tl.prePersist();
        assertNotNull(tl.getTransactionDate());

        // Cover TransactionLog.prePersist when status is null
        TransactionLog tlNullStatus = new TransactionLog();
        tlNullStatus.prePersist();
        assertEquals(TransactionStatus.PENDING, tlNullStatus.getStatus());

        // Cover RewardAccount.prePersist when totalPoints is null
        RewardAccount raNullPoints = new RewardAccount();
        raNullPoints.prePersist();
        assertEquals(0, raNullPoints.getTotalPoints());

        // Cover RewardLedger.prePersist when grantedAt is null
        RewardLedger rlNullGranted = new RewardLedger();
        rlNullGranted.prePersist();
        assertNotNull(rlNullGranted.getGrantedAt());

        // ----- Additional Account Validations -----
        Account accountValid = Account.builder().holderName("John").balance(BigDecimal.TEN).status(AccountStatus.ACTIVE).build();

        // 1. prePersist throw negative balance
        Account accNeg = Account.builder().holderName("John").balance(new BigDecimal("-10")).status(AccountStatus.ACTIVE).build();
        assertThrows(IllegalArgumentException.class, accNeg::prePersist);

        // 2. prePersist throw empty/null holderName
        Account accNoHolder = Account.builder().holderName("").balance(BigDecimal.TEN).status(AccountStatus.ACTIVE).build();
        assertThrows(IllegalArgumentException.class, accNoHolder::prePersist);

        // 3. debit validation null/zero/negative amount
        assertThrows(IllegalArgumentException.class, () -> accountValid.debit(null));
        assertThrows(IllegalArgumentException.class, () -> accountValid.debit(BigDecimal.ZERO));

        // 4. debit validation exceeds max limit
        assertThrows(IllegalArgumentException.class, () -> accountValid.debit(new BigDecimal("1000001")));

        // 5. debit validation insufficient balance
        assertThrows(InsufficientBalanceException.class, () -> accountValid.debit(new BigDecimal("50")));

        // 6. debit validation when LOCKED
        Account accLocked = Account.builder().holderName("John").balance(BigDecimal.TEN).status(AccountStatus.LOCKED).build();
        assertThrows(InactiveAccountException.class, () -> accLocked.debit(BigDecimal.ONE));

        // 7. debit validation when CLOSED
        Account accClosed = Account.builder().holderName("John").balance(BigDecimal.TEN).status(AccountStatus.CLOSED).build();
        assertThrows(InactiveAccountException.class, () -> accClosed.debit(BigDecimal.ONE));

        // 8. close validation when non-zero balance
        Account accNonZero = Account.builder().holderName("John").balance(BigDecimal.TEN).status(AccountStatus.ACTIVE).build();
        assertThrows(IllegalStateException.class, accNonZero::close);

        // 9. activate validation when CLOSED
        Account accClosedToActivate = Account.builder().holderName("John").balance(BigDecimal.ZERO).status(AccountStatus.CLOSED).build();
        assertThrows(IllegalStateException.class, accClosedToActivate::activate);

        // 10. helper getters
        assertTrue(accountValid.isActive());
        assertFalse(accountValid.isLocked());
        assertFalse(accountValid.isClosed());

        accountValid.lock();
        assertTrue(accountValid.isLocked());

        Account closedAccount = Account.builder().holderName("John").balance(BigDecimal.ZERO).status(AccountStatus.CLOSED).build();
        assertTrue(closedAccount.isClosed());

        // --- Additional Entity Coverage ---
        // Account Constructors & Builder methods
        Account accDefault = new Account();
        assertNull(accDefault.getId());

        Account.AccountBuilder ab = Account.builder();
        ab.version(1);
        ab.createdAt(now);
        ab.lastUpdated(now);
        Account builtAcc = ab.build();
        assertEquals(1, builtAcc.getVersion());

        // Account credit and prePersist nulls
        Account accNulls = Account.builder().holderName("Jane").status(AccountStatus.ACTIVE).build();
        accNulls.prePersist();
        assertEquals(BigDecimal.ZERO, accNulls.getBalance());
        assertNotNull(accNulls.getCreatedAt());

        accNulls.credit(BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, accNulls.getBalance());

        // Account validateAccountIsActive when status is null
        Account accNullStatus = Account.builder().holderName("Jane").balance(BigDecimal.TEN).status(null).build();
        assertThrows(InactiveAccountException.class, () -> accNullStatus.debit(BigDecimal.ONE));

        // BankDetails Default constructor & prePersist when createdAt is non-null
        BankDetails bdDefault = new BankDetails();
        assertNull(bdDefault.getId());

        BankDetails bdPre = BankDetails.builder().createdAt(now).build();
        bdPre.prePersist();
        assertEquals(now, bdPre.getCreatedAt());

        // RewardAccount prePersist when createdAt is non-null
        RewardAccount raPre = new RewardAccount(1L);
        raPre.setCreatedAt(now);
        raPre.prePersist();
        assertEquals(now, raPre.getCreatedAt());

        // RewardLedger getters and setters & Builder builder methods
        RewardLedger rlDefault = new RewardLedger();
        rlDefault.setId(100L);
        rlDefault.setAccountId(200L);
        rlDefault.setTransactionId(300L);
        rlDefault.setTransactionAmount(BigDecimal.TEN);
        rlDefault.setPointsAwarded(5);
        rlDefault.setDescription("desc");
        rlDefault.setGrantedAt(now);

        assertEquals(100L, rlDefault.getId());
        assertEquals(200L, rlDefault.getAccountId());
        assertEquals(300L, rlDefault.getTransactionId());
        assertEquals(BigDecimal.TEN, rlDefault.getTransactionAmount());
        assertEquals(5, rlDefault.getPointsAwarded());
        assertEquals("desc", rlDefault.getDescription());
        assertEquals(now, rlDefault.getGrantedAt());

        RewardLedger.Builder rlb = RewardLedger.builder();
        rlb.accountId(1L);
        rlb.transactionId(2L);
        rlb.transactionAmount(BigDecimal.ONE);
        rlb.pointsAwarded(10);
        rlb.description("desc");
        rlb.grantedAt(now);
        RewardLedger builtRl = rlb.build();
        assertEquals(1L, builtRl.getAccountId());

        // Extra updates to cover remaining methods and branches:
        // 2. RewardAccount preUpdate, setters
        raPre.preUpdate();
        raPre.setAccountId(10L);
        raPre.setTotalPoints(100);
        assertEquals(10L, raPre.getAccountId());
        assertEquals(100, raPre.getTotalPoints());

        // 3. BankDetails setLastUpdated
        bdPre.setLastUpdated(now);

        // 4. Account setVersion, setLastUpdated, prePersist with non-null createdAt, prePersist with null holderName
        builtAcc.setVersion(2);
        assertEquals(2, builtAcc.getVersion());
        builtAcc.setLastUpdated(now);
        assertEquals(now, builtAcc.getLastUpdated());

        Account accWithCreatedAt = Account.builder().holderName("John").createdAt(now).build();
        accWithCreatedAt.prePersist();
        assertEquals(now, accWithCreatedAt.getCreatedAt());

        Account accNullHolder = Account.builder().holderName(null).build();
        assertThrows(IllegalArgumentException.class, accNullHolder::prePersist);

        // 5. TransactionLog prePersist both non-null
        TransactionLog tlBothNonNull = TransactionLog.builder().transactionDate(now).status(TransactionStatus.SUCCESS).build();
        tlBothNonNull.prePersist();
        assertEquals(now, tlBothNonNull.getTransactionDate());
        assertEquals(TransactionStatus.SUCCESS, tlBothNonNull.getStatus());

        // 6. RewardLedger prePersist non-null
        RewardLedger rlPreNonNull = new RewardLedger();
        rlPreNonNull.setGrantedAt(now);
        rlPreNonNull.prePersist();
        assertEquals(now, rlPreNonNull.getGrantedAt());

        // 7. AccountNumberGenerator constructor
        assertNotNull(new com.company.mts.utils.AccountNumberGenerator());

        // 8. AuthUser prePersist non-null
        AuthUser userPreNonNull = new AuthUser();
        userPreNonNull.setCreatedAt(now);
        userPreNonNull.prePersist();
        assertEquals(now, userPreNonNull.getCreatedAt());

        // 9. JwtAuthenticationEntryPoint commencement
        com.company.mts.config.JwtAuthenticationEntryPoint entryPoint = new com.company.mts.config.JwtAuthenticationEntryPoint();
        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        jakarta.servlet.http.HttpServletResponse response = mock(jakarta.servlet.http.HttpServletResponse.class);
        org.springframework.security.core.AuthenticationException authException = mock(org.springframework.security.core.AuthenticationException.class);
        when(authException.getMessage()).thenReturn("Unauthorized Access");

        java.io.StringWriter stringWriter = new java.io.StringWriter();
        java.io.PrintWriter writer = new java.io.PrintWriter(stringWriter);
        try {
            when(response.getWriter()).thenReturn(writer);
            entryPoint.commence(request, response, authException);
        } catch (Exception e) {
            fail("commence should not throw exception: " + e.getMessage());
        }

        org.mockito.Mockito.verify(response).setContentType("application/json;charset=UTF-8");
        org.mockito.Mockito.verify(response).setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(stringWriter.toString().contains("Unauthorized Access"));
    }
}
