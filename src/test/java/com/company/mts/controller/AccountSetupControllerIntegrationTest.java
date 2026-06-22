package com.company.mts.controller;

import com.company.mts.dto.AccountSetupRequest;
import com.company.mts.entity.Account;
import com.company.mts.entity.AuthUser;
import com.company.mts.entity.BankDetails;
import com.company.mts.repository.AccountRepository;
import com.company.mts.repository.AuthUserRepository;
import com.company.mts.service.BankDetailsService;
import com.company.mts.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class AccountSetupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountSetupController accountSetupController;

    @MockBean
    private BankDetailsService bankDetailsService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private AuthUserRepository authUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private AccountSetupRequest validRequest;
    private BankDetails testDetails;

    @BeforeEach
    void setUp() {
        validRequest = new AccountSetupRequest();
        validRequest.setUserName("johndoe");
        validRequest.setAccountNumber("123456789012");
        validRequest.setBankName("HDFC Bank");
        validRequest.setIfscCode("HDFC0000123");
        validRequest.setBranchName("Downtown");
        validRequest.setAddress("123 Street");
        validRequest.setEmail("john@test.com");
        validRequest.setPhoneNumber("9876543210");
        validRequest.setCreditCardNumber("1111222233334444");
        validRequest.setCvv("123");
        validRequest.setExpiryDate("12/28");

        testDetails = BankDetails.builder()
                .id(1L)
                .userName("johndoe")
                .accountNumber("123456789012")
                .bankName("HDFC Bank")
                .upiId("johndoe@fihdfc")
                .build();
    }

    @Test
    void create_MissingUserName_ReturnsBadRequest() throws Exception {
        validRequest.setUserName("");

        mockMvc.perform(post("/api/v1/account-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not logged in or session expired. Please log in and try again."));
    }

    @Test
    void create_NewDetails_Success() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.empty());
        when(bankDetailsService.findByAccountNumber("123456789012")).thenReturn(Optional.empty());
        when(bankDetailsService.save(any(BankDetails.class))).thenReturn(testDetails);
        when(accountRepository.existsByAccountNumber("123456789012")).thenReturn(false);

        AuthUser user = new AuthUser();
        user.setName("johndoe");
        user.setFirstLogin(true);
        when(authUserRepository.findByNameIgnoreCase("johndoe")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/account-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.upiId").value("johndoe@fihdfc"));

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(authUserRepository, times(1)).save(user);
        assertEquals(false, user.isFirstLogin());
    }

    @Test
    void create_ExistingDetails_Reuses() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.of(testDetails));
        when(accountRepository.existsByAccountNumber("123456789012")).thenReturn(true);
        when(authUserRepository.findByNameIgnoreCase("johndoe")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/account-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(bankDetailsService, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void create_ExistingDetailsConflict_ReturnsConflict() throws Exception {
        testDetails.setUserName("differentuser");
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.of(testDetails));

        mockMvc.perform(post("/api/v1/account-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Account setup already exists for this user or account number. Please contact support."));
    }

    @Test
    void create_DataIntegrityViolation_ReturnsConflict() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.empty());
        when(bankDetailsService.findByAccountNumber("123456789012")).thenReturn(Optional.empty());
        when(bankDetailsService.save(any(BankDetails.class))).thenThrow(new DataIntegrityViolationException("unique index violation"));

        mockMvc.perform(post("/api/v1/account-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Account setup already exists for this user or account number. Please contact support."));
    }

    @Test
    void create_GeneralException_ReturnsInternalError() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.empty());
        when(bankDetailsService.findByAccountNumber("123456789012")).thenReturn(Optional.empty());
        when(bankDetailsService.save(any(BankDetails.class))).thenThrow(new RuntimeException("Something failed"));

        mockMvc.perform(post("/api/v1/account-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something failed"));
    }

    @Test
    void create_UpiSuffixResolutionTests() throws Exception {
        // Test different banks
        String[] banks = {"ICICI", "SBI", "AXIS", "Other Bank"};
        String[] expectedUpi = {"johndoe@icici", "johndoe@oksbi", "johndoe@axisbank", "johndoe@upi"};

        for (int i = 0; i < banks.length; i++) {
            validRequest.setBankName(banks[i]);
            BankDetails details = BankDetails.builder()
                    .id((long) (i + 10))
                    .userName("johndoe")
                    .accountNumber("123456789012")
                    .upiId(expectedUpi[i])
                    .build();

            when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.empty());
            when(bankDetailsService.findByAccountNumber("123456789012")).thenReturn(Optional.empty());
            when(bankDetailsService.save(any(BankDetails.class))).thenReturn(details);
            when(accountRepository.existsByAccountNumber("123456789012")).thenReturn(true);

            mockMvc.perform(post("/api/v1/account-setup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.upiId").value(expectedUpi[i]));
        }
    }

    @Test
    void getByAccountNumber_Found() throws Exception {
        when(bankDetailsService.findByAccountNumber("123456789012")).thenReturn(Optional.of(testDetails));

        mockMvc.perform(get("/api/v1/account-setup/123456789012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("123456789012"));
    }

    @Test
    void getByAccountNumber_NotFound_ReturnsException() throws Exception {
        when(bankDetailsService.findByAccountNumber("123456789012")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/account-setup/123456789012"))
                .andExpect(status().isBadRequest()); // handled as IllegalArgumentException
    }

    @Test
    void setupUpi_Success() throws Exception {
        when(bankDetailsService.setupUpi(1L, "new@upi")).thenReturn(testDetails);
        testDetails.setUpiId("new@upi");

        Map<String, String> body = new HashMap<>();
        body.put("upiId", "new@upi");

        mockMvc.perform(post("/api/v1/account-setup/1/upi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upiId").value("new@upi"));
    }

    @Test
    void getByUserName_Found() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.of(testDetails));

        mockMvc.perform(get("/api/v1/account-setup/user/johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("johndoe"));
    }

    @Test
    void getByUserName_NotFound() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/account-setup/user/johndoe"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_Found_Success() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.of(testDetails));
        when(bankDetailsService.save(any(BankDetails.class))).thenReturn(testDetails);

        mockMvc.perform(put("/api/v1/account-setup/user/johndoe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("johndoe"));
    }

    @Test
    void update_NotFound() throws Exception {
        when(bankDetailsService.findByUserName("johndoe")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/account-setup/user/johndoe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setPin_Success() throws Exception {
        when(bankDetailsService.updatePin("johndoe", "1234")).thenReturn(testDetails);

        Map<String, String> body = new HashMap<>();
        body.put("pin", "1234");

        mockMvc.perform(post("/api/v1/account-setup/user/johndoe/pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void setPin_InvalidPin_ThrowsException() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("pin", "12"); // invalid length

        mockMvc.perform(post("/api/v1/account-setup/user/johndoe/pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otpFlow_Success() throws Exception {
        // Send OTP (email contact)
        Map<String, String> body = new HashMap<>();
        body.put("contact", "john@test.com");

        doNothing().when(emailService).sendOtpEmail(eq("john@test.com"), anyString());

        mockMvc.perform(post("/api/v1/account-setup/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.contact").value("john@test.com"));

        // Send OTP (email service throws exception, should still succeed with log warning)
        doThrow(new RuntimeException("Mail server down")).when(emailService).sendOtpEmail(eq("john@test.com"), anyString());
        mockMvc.perform(post("/api/v1/account-setup/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Send OTP (non-email contact)
        body.put("contact", "1234567890");
        mockMvc.perform(post("/api/v1/account-setup/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Verify OTP - Incorrect OTP
        Map<String, String> verifyBody = new HashMap<>();
        verifyBody.put("contact", "john@test.com");
        verifyBody.put("otp", "000000");

        mockMvc.perform(post("/api/v1/account-setup/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));

        // Inject correct OTP into memory store and verify success
        @SuppressWarnings("unchecked")
        Map<String, String> otpStore = (Map<String, String>) org.springframework.test.util.ReflectionTestUtils.getField(accountSetupController, "otpStore");
        assertNotNull(otpStore);
        otpStore.put("john@test.com", "123456");

        verifyBody.put("otp", "123456");
        mockMvc.perform(post("/api/v1/account-setup/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void testPrivateUpiGeneration_NullChecks() {
        String upiNullUser = (String) org.springframework.test.util.ReflectionTestUtils.invokeMethod(accountSetupController, "generateUpiId", null, "HDFC");
        assertNull(upiNullUser);

        String upiNullBank = (String) org.springframework.test.util.ReflectionTestUtils.invokeMethod(accountSetupController, "generateUpiId", "johndoe", null);
        assertNull(upiNullBank);
    }
}
