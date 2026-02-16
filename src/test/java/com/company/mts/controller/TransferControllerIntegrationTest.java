package com.company.mts.controller;

import com.company.mts.dto.IdempotentTransferRequest;
import com.company.mts.dto.TransactionDTO;
import com.company.mts.entity.TransactionStatus;
import com.company.mts.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransferControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private TransactionService transactionService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void executeTransfer_Success() throws Exception {
                IdempotentTransferRequest request = new IdempotentTransferRequest();
                request.setFromAccountId(1L);
                request.setToAccountId(2L);
                request.setAmount(new BigDecimal("100.00"));
                request.setIdempotencyKey("KEY123");

                TransactionDTO dto = TransactionDTO.builder()
                                .id(100L)
                                .status(TransactionStatus.SUCCESS)
                                .amount(new BigDecimal("100.00"))
                                .build();

                when(transactionService.executeIdempotentTransfer(any(), any(), any(), any(), any()))
                                .thenReturn(dto);

                mockMvc.perform(post("/api/v1/transfers/idempotent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Idempotency-Key", "KEY123")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("SUCCESS"));
        }
}
