package com.company.mts.controller;

import com.company.mts.dto.AccountBalanceDTO;
import com.company.mts.dto.TransferByAccountNumberRequest;
import com.company.mts.entity.Account;
import com.company.mts.entity.AccountStatus;
import com.company.mts.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .accountNumber("123456789012")
                .holderName("John Doe")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void createAccount_Success() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setHolderName("John Doe");
        request.setInitialBalance(new BigDecimal("1000.00"));

        when(accountService.createAccount(eq("John Doe"), any(BigDecimal.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holderName").value("John Doe"))
                .andExpect(jsonPath("$.accountNumber").value("123456789012"));
    }

    @Test
    void getAccount_Success() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(testAccount);

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.holderName").value("John Doe"));
    }

    @Test
    void getAccountBalance_Success() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(testAccount);

        mockMvc.perform(get("/api/v1/accounts/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAccountByNumber_Success() throws Exception {
        when(accountService.getAccountByAccountNumber("123456789012")).thenReturn(testAccount);

        mockMvc.perform(get("/api/v1/accounts/number/123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("123456789012"));
    }

    @Test
    void getAccountByHolderName_Success() throws Exception {
        when(accountService.getAccountByHolderName("John Doe")).thenReturn(testAccount);

        mockMvc.perform(get("/api/v1/accounts/holder/John Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holderName").value("John Doe"));
    }

    @Test
    void creditAccount_Success() throws Exception {
        AmountRequest request = new AmountRequest();
        request.setAmount(new BigDecimal("500.00"));

        testAccount.setBalance(new BigDecimal("1500.00"));
        when(accountService.credit(eq(1L), any(BigDecimal.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/v1/accounts/1/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.00));
    }

    @Test
    void debitAccount_Success() throws Exception {
        AmountRequest request = new AmountRequest();
        request.setAmount(new BigDecimal("300.00"));

        testAccount.setBalance(new BigDecimal("700.00"));
        when(accountService.debit(eq(1L), any(BigDecimal.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/v1/accounts/1/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.00));
    }

    @Test
    void transfer_Success() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("300.00"));

        doNothing().when(accountService).transfer(eq(1L), eq(2L), any(BigDecimal.class));

        mockMvc.perform(post("/api/v1/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer successful"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void transferByAccountNumber_Success() throws Exception {
        TransferByAccountNumberRequest request = new TransferByAccountNumberRequest();
        request.setFromAccountNumber("123456789012");
        request.setToAccountNumber("234567890123");
        request.setAmount(new BigDecimal("200.00"));

        doNothing().when(accountService).transferByAccountNumber(eq("123456789012"), eq("234567890123"), any(BigDecimal.class));

        mockMvc.perform(post("/api/v1/accounts/transfer/by-number")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer successful"))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void lockAccount_Success() throws Exception {
        testAccount.setStatus(AccountStatus.LOCKED);
        when(accountService.lockAccount(1L)).thenReturn(testAccount);

        mockMvc.perform(post("/api/v1/accounts/1/lock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    void unlockAccount_Success() throws Exception {
        testAccount.setStatus(AccountStatus.ACTIVE);
        when(accountService.unlockAccount(1L)).thenReturn(testAccount);

        mockMvc.perform(post("/api/v1/accounts/1/unlock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void closeAccount_Success() throws Exception {
        testAccount.setStatus(AccountStatus.CLOSED);
        when(accountService.closeAccount(1L)).thenReturn(testAccount);

        mockMvc.perform(post("/api/v1/accounts/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void transferResponse_GettersSetters() {
        AccountController.TransferResponse resp = new AccountController.TransferResponse("msg", new BigDecimal("10.0"), false);
        resp.setMessage("newMsg");
        resp.setAmount(new BigDecimal("20.0"));
        resp.setSuccess(true);

        assertEquals("newMsg", resp.getMessage());
        assertEquals(new BigDecimal("20.0"), resp.getAmount());
        assertTrue(resp.isSuccess());
    }
}
