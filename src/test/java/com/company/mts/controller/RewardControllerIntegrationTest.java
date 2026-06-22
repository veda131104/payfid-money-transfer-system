package com.company.mts.controller;

import com.company.mts.dto.RewardLedgerDTO;
import com.company.mts.dto.RewardSummaryDTO;
import com.company.mts.service.RewardService;
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
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class RewardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewardService rewardService;

    private RewardSummaryDTO testSummary;
    private RewardLedgerDTO testLedger;

    @BeforeEach
    void setUp() {
        testSummary = new RewardSummaryDTO();
        testSummary.setRewardAccountId(100L);
        testSummary.setAccountId(10L);
        testSummary.setTotalPoints(50);
        testSummary.setCreatedAt(LocalDateTime.now());
        testSummary.setUpdatedAt(LocalDateTime.now());

        testLedger = new RewardLedgerDTO();
        testLedger.setId(200L);
        testLedger.setAccountId(10L);
        testLedger.setTransactionId(1L);
        testLedger.setTransactionAmount(new BigDecimal("150.00"));
        testLedger.setPointsAwarded(1);
        testLedger.setDescription("Description");
        testLedger.setGrantedAt(LocalDateTime.now());
    }

    @Test
    void getRewardSummary_Success() throws Exception {
        when(rewardService.getRewardSummary(10L)).thenReturn(testSummary);

        mockMvc.perform(get("/api/v1/rewards/10/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardAccountId").value(100))
                .andExpect(jsonPath("$.totalPoints").value(50));
    }

    @Test
    void getRewardHistory_Success() throws Exception {
        when(rewardService.getRewardHistory(10L)).thenReturn(Collections.singletonList(testLedger));

        mockMvc.perform(get("/api/v1/rewards/10/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200))
                .andExpect(jsonPath("$[0].pointsAwarded").value(1));
    }

    @Test
    void evaluateReward_Eligible_ReturnsOk() throws Exception {
        when(rewardService.evaluateAndGrantReward(1L)).thenReturn(testLedger);

        mockMvc.perform(post("/api/v1/rewards/evaluate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200));
    }

    @Test
    void evaluateReward_NotEligible_ReturnsNoContent() throws Exception {
        when(rewardService.evaluateAndGrantReward(1L)).thenReturn(null);

        mockMvc.perform(post("/api/v1/rewards/evaluate/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void initializeRewardAccount_Success() throws Exception {
        when(rewardService.getOrCreateRewardAccount(10L)).thenReturn(testSummary);

        mockMvc.perform(post("/api/v1/rewards/10/initialize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardAccountId").value(100));
    }

    @Test
    void health_Success() throws Exception {
        mockMvc.perform(get("/api/v1/rewards/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.module").value("RewardModule"));
    }
}
