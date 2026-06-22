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

import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import com.company.mts.dto.TransferByAccountNumberRequest;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
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

        @Test
        void executeTransferByAccountNumber_Success() throws Exception {
                TransferByAccountNumberRequest request = new TransferByAccountNumberRequest();
                request.setFromAccountNumber("123456789012");
                request.setToAccountNumber("234567890123");
                request.setAmount(new BigDecimal("100.00"));

                TransactionDTO dto = TransactionDTO.builder()
                                .id(101L)
                                .status(TransactionStatus.SUCCESS)
                                .amount(new BigDecimal("100.00"))
                                .build();

                when(transactionService.executeTransferByAccountNumber(anyString(), anyString(), any(), anyString()))
                                .thenReturn(dto);

                mockMvc.perform(post("/api/v1/transfers/by-account")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.transactionId").value(101L))
                                .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        void getAllTransactions_Success() throws Exception {
                TransactionDTO dto = TransactionDTO.builder().id(100L).build();
                when(transactionService.getAllTransactions()).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/v1/transfers"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(100L));
        }

        @Test
        void getTransactionById_Success() throws Exception {
                TransactionDTO dto = TransactionDTO.builder().id(100L).build();
                when(transactionService.getTransactionById(100L)).thenReturn(dto);

                mockMvc.perform(get("/api/v1/transfers/100"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(100L));
        }

        @Test
        void getAccountTransactionHistory_Success() throws Exception {
                TransactionDTO dto = TransactionDTO.builder().id(100L).build();
                when(transactionService.getAccountTransactionHistory(1L)).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/v1/transfers/account/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accountId").value(1L))
                                .andExpect(jsonPath("$.totalTransactions").value(1));
        }

        @Test
        void getFailedTransactions_Success() throws Exception {
                TransactionDTO dto = TransactionDTO.builder().id(100L).build();
                when(transactionService.getFailedTransactions(1L)).thenReturn(List.of(dto));

                mockMvc.perform(get("/api/v1/transfers/account/1/failed"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(100L));
        }

        @Test
        void testResponseDTOs() {
                TransferController.TransferResponse resp = new TransferController.TransferResponse("msg", 1L, BigDecimal.TEN, "status", "key");
                resp.setMessage("newMsg");
                resp.setTransactionId(2L);
                resp.setAmount(BigDecimal.ONE);
                resp.setStatus("newStatus");
                resp.setIdempotencyKey("newKey");

                assertEquals("newMsg", resp.getMessage());
                assertEquals(2L, resp.getTransactionId());
                assertEquals(BigDecimal.ONE, resp.getAmount());
                assertEquals("newStatus", resp.getStatus());
                assertEquals("newKey", resp.getIdempotencyKey());

                TransferController.TransactionHistoryResponse hist = new TransferController.TransactionHistoryResponse(1L, 1, Collections.emptyList());
                hist.setAccountId(2L);
                hist.setTotalTransactions(2);
                hist.setTransactions(Collections.emptyList());

                assertEquals(2L, hist.getAccountId());
                assertEquals(2, hist.getTotalTransactions());
                assertEquals(0, hist.getTransactions().size());
        }
}
