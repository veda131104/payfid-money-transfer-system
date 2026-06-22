package com.company.mts.controller;

import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.SignupRequest;
import com.company.mts.dto.TokenRequest;
import com.company.mts.dto.ResetPasswordRequest;
import com.company.mts.dto.ForgotPasswordRequest;
import com.company.mts.dto.CredentialsResponse;
import com.company.mts.entity.AuthUser;
import com.company.mts.service.AuthService;
import com.company.mts.security.CryptoService;
import com.company.mts.security.JwtTokenProvider;
import com.company.mts.repository.AuthUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    private AuthUserRepository authUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signup_Success() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setName("bob");
        request.setPassword("password123");
        request.setEmail("bob@test.com");

        AuthUser user = new AuthUser();
        user.setName("bob");

        when(authService.signup(any())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("bob"));
    }

    @Test
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setName("bob");
        request.setPassword("password123");

        AuthUser user = new AuthUser();
        user.setName("bob");

        when(authService.login(any())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("bob"));
    }

    @Test
    void signup_Failure() throws Exception {
        SignupRequest request = new SignupRequest();
        request.setName("bob");
        request.setEmail("bob@test.com");
        request.setPassword("password123");
        when(authService.signup(any())).thenThrow(new RuntimeException("Signup error"));

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void login_Failure() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setName("bob");
        when(authService.login(any())).thenThrow(new RuntimeException("Login error"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void loginWithToken_Success() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setToken("token123");

        AuthUser user = new AuthUser();
        user.setName("bob");
        when(authService.loginWithToken("token123")).thenReturn(user);
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt");

        mockMvc.perform(post("/api/v1/auth/login-with-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void loginWithToken_Failure() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setToken("invalid");
        when(authService.loginWithToken("invalid")).thenThrow(new IllegalArgumentException("Invalid token"));

        mockMvc.perform(post("/api/v1/auth/login-with-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCredentials_Success() throws Exception {
        CredentialsResponse creds = new CredentialsResponse("bob", "pwd");
        when(authService.getCredentialsByToken("token123")).thenReturn(creds);

        mockMvc.perform(get("/api/v1/auth/credentials/token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("bob"));
    }

    @Test
    void getCredentials_Failure() throws Exception {
        when(authService.getCredentialsByToken("invalid")).thenThrow(new RuntimeException("Expired"));

        mockMvc.perform(get("/api/v1/auth/credentials/invalid"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setName("bob");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_Failure() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setName("bob");
        doThrow(new RuntimeException("Mail error")).when(authService).processForgotPassword("bob");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void verifyToken_Success() throws Exception {
        AuthUser user = new AuthUser();
        user.setName("bob");
        when(authService.loginWithToken("token123")).thenReturn(user);
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt");

        mockMvc.perform(get("/api/v1/auth/verify-token")
                .param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void verifyToken_Failure() throws Exception {
        when(authService.loginWithToken("invalid")).thenThrow(new IllegalArgumentException("Expired"));

        mockMvc.perform(get("/api/v1/auth/verify-token")
                .param("token", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token123");
        request.setNewPassword("password123");

        AuthUser user = new AuthUser();
        user.setName("bob");
        when(authService.resetPassword(any())).thenReturn(user);
        when(jwtTokenProvider.generateToken(user)).thenReturn("jwt");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }

    @Test
    void resetPassword_Failure() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid");
        request.setNewPassword("password123");
        when(authService.resetPassword(any())).thenThrow(new IllegalArgumentException("Expired"));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
