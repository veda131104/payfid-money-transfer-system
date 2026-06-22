package com.company.mts.controller;

import com.company.mts.service.AnalyticsService;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.AuthUserRepository;
import com.company.mts.entity.AuthUser;
import com.company.mts.entity.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private AuthUserRepository authUserRepository;

    @MockBean
    private AccountRepository accountRepository;

    @Test
    void getVolume_Success() throws Exception {
        AuthUser user = new AuthUser();
        user.setName("bob");
        user.setRole("USER");

        Account acc = Account.builder().holderName("Bob Smith").build();

        when(authUserRepository.findByNameIgnoreCase("bob")).thenReturn(Optional.of(user));
        when(accountRepository.findByHolderNameIgnoreCase(anyString())).thenReturn(Optional.of(acc));
        when(analyticsService.getTransactionVolume(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "bob"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatus_Success() throws Exception {
        when(analyticsService.checkStatus()).thenReturn(Collections.singletonMap("status", "healthy"));

        mockMvc.perform(get("/api/v1/analytics/status"))
                .andExpect(status().isOk());
    }

    @Test
    void getVolume_ContextBlankOrUndefined_FallsBackToDefaultAdmin() throws Exception {
        when(analyticsService.getTransactionVolume("veda131104", "ADMIN", "Veda Jagannath"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "undefined"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", ""))
                .andExpect(status().isOk());
    }

    @Test
    void getVolume_UserNotFound_FallsBackToDefaultAdmin() throws Exception {
        when(authUserRepository.findByNameIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(analyticsService.getTransactionVolume("veda131104", "ADMIN", "Veda Jagannath"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "unknown"))
                .andExpect(status().isOk());
    }

    @Test
    void getVolume_UserFound_NoMatchingAccount() throws Exception {
        AuthUser user = new AuthUser();
        user.setName("bob");
        user.setRole("USER");
        when(authUserRepository.findByNameIgnoreCase("bob")).thenReturn(Optional.of(user));
        when(accountRepository.findAll()).thenReturn(Collections.emptyList());
        when(analyticsService.getTransactionVolume("bob", "USER", "bob"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "bob"))
                .andExpect(status().isOk());
    }

    @Test
    void getVolume_UserFound_PartialMatchAccount() throws Exception {
        AuthUser user = new AuthUser();
        user.setName("bob");
        user.setRole("USER");
        when(authUserRepository.findByNameIgnoreCase("bob")).thenReturn(Optional.of(user));

        Account acc = Account.builder().holderName("Bobby").build();
        when(accountRepository.findAll()).thenReturn(Collections.singletonList(acc));
        when(analyticsService.getTransactionVolume("bob", "USER", "Bobby"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "bob"))
                .andExpect(status().isOk());
    }

    @Test
    void getAccountActivity_Success() throws Exception {
        AuthUser user = new AuthUser();
        user.setName("bob");
        user.setRole("USER");
        when(authUserRepository.findByNameIgnoreCase("bob")).thenReturn(Optional.of(user));
        when(accountRepository.findAll()).thenReturn(Collections.emptyList());
        when(analyticsService.getAccountActivity("bob", "USER", "bob"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/account-activity")
                .param("name", "bob"))
                .andExpect(status().isOk());
    }

    @Test
    void getSuccessRate_Success() throws Exception {
        AuthUser user = new AuthUser();
        user.setName("bob");
        user.setRole("USER");
        when(authUserRepository.findByNameIgnoreCase("bob")).thenReturn(Optional.of(user));
        when(accountRepository.findAll()).thenReturn(Collections.emptyList());
        when(analyticsService.getSuccessRate("bob", "USER", "bob"))
                .thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/v1/analytics/success-rate")
                .param("name", "bob"))
                .andExpect(status().isOk());
    }

    @Test
    void getPeakHours_Success() throws Exception {
        AuthUser user = new AuthUser();
        user.setName("bob");
        user.setRole("USER");
        when(authUserRepository.findByNameIgnoreCase("bob")).thenReturn(Optional.of(user));
        when(accountRepository.findAll()).thenReturn(Collections.emptyList());
        when(analyticsService.getPeakHours("bob", "USER", "bob"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/peak-hours")
                .param("name", "bob"))
                .andExpect(status().isOk());
    }

    @Test
    void testExtraAnalyticsControllerBranches() throws Exception {
        // 1. name is null
        mockMvc.perform(get("/api/v1/analytics/transaction-volume"))
                .andExpect(status().isOk());

        // 2. name is "User"
        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "User"))
                .andExpect(status().isOk());

        // 3. name is blank spaces
        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "   "))
                .andExpect(status().isOk());

        // 4. User found, matching logic with null names and partial name containment
        AuthUser user1 = new AuthUser();
        user1.setName("bo");
        user1.setRole("USER");
        when(authUserRepository.findByNameIgnoreCase("bo")).thenReturn(Optional.of(user1));

        Account accNullName = Account.builder().holderName(null).build();
        Account accContain1 = Account.builder().holderName("bob").build();
        when(accountRepository.findAll()).thenReturn(java.util.List.of(accNullName, accContain1));
        when(analyticsService.getTransactionVolume("bo", "USER", "bob"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "bo"))
                .andExpect(status().isOk());

        // 5. User found, other partial containment (user name contains account holder name)
        AuthUser user2 = new AuthUser();
        user2.setName("bobby");
        user2.setRole("USER");
        when(authUserRepository.findByNameIgnoreCase("bobby")).thenReturn(Optional.of(user2));

        Account accContain2 = Account.builder().holderName("bob").build();
        when(accountRepository.findAll()).thenReturn(java.util.List.of(accContain2));
        when(analyticsService.getTransactionVolume("bobby", "USER", "bob"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/transaction-volume")
                .param("name", "bobby"))
                .andExpect(status().isOk());
    }
}
