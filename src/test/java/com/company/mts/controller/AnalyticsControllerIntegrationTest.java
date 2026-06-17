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

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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
}
