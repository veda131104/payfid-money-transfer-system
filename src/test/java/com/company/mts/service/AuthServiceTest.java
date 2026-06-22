package com.company.mts.service;

import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.SignupRequest;
import com.company.mts.dto.CredentialsResponse;
import com.company.mts.dto.ResetPasswordRequest;
import com.company.mts.entity.AuthUser;
import com.company.mts.exception.DuplicateUserException;
import com.company.mts.repository.AuthUserRepository;
import com.company.mts.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private AuthUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AuthUser();
        testUser.setId(1L);
        testUser.setName("testuser");
        testUser.setPassword("password123");
        testUser.setEmail("test@example.com");
    }

    @Test
    void signup_Success() {
        SignupRequest request = new SignupRequest();
        request.setName("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        when(authUserRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(authUserRepository.existsByNameIgnoreCase("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(testUser);

        AuthUser result = authService.signup(request);

        assertNotNull(result);
        assertEquals("testuser", result.getName());
        verify(authUserRepository).save(any(AuthUser.class));
    }

    @Test
    void signup_DuplicateUser_ThrowsException() {
        SignupRequest request = new SignupRequest();
        request.setName("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        when(authUserRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.signup(request));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setName("testuser");
        request.setPassword("password123");

        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "password123")).thenReturn(true);

        AuthUser result = authService.login(request);

        assertNotNull(result);
        assertEquals("testuser", result.getName());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setName("testuser");
        request.setPassword("wrongpassword");

        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "password123")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void loginWithRememberMe_SetsToken() {
        LoginRequest request = new LoginRequest();
        request.setName("testuser");
        request.setPassword("password123");
        request.setRememberMe(true);

        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "password123")).thenReturn(true);

        AuthUser result = authService.login(request);

        assertNotNull(result.getRememberToken());
        assertNotNull(result.getRememberTokenExpiry());
        verify(authUserRepository).save(testUser);
    }

    @Test
    void processForgotPassword_SendsEmail() {
        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

        authService.processForgotPassword("testuser");

        assertNotNull(testUser.getRecoveryToken());
        verify(emailService).sendForgotPasswordEmail(eq("test@example.com"), eq("testuser"), anyString());
    }

    @Test
    void signup_DuplicateName_ThrowsException() {
        SignupRequest request = new SignupRequest();
        request.setName("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        when(authUserRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(authUserRepository.existsByNameIgnoreCase("testuser")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.signup(request));
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setName("unknown");
        request.setPassword("password");

        when(authUserRepository.findByNameIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void getCredentialsByToken_Success() {
        testUser.setRememberToken("token123");
        testUser.setRememberTokenExpiry(LocalDateTime.now().plusDays(1));
        when(authUserRepository.findByRememberToken("token123")).thenReturn(Optional.of(testUser));

        CredentialsResponse result = authService.getCredentialsByToken("token123");

        assertNotNull(result);
        assertEquals("testuser", result.getName());
        assertEquals("password123", result.getPassword());
    }

    @Test
    void getCredentialsByToken_NotFound_ThrowsException() {
        when(authUserRepository.findByRememberToken("invalid")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.getCredentialsByToken("invalid"));
    }

    @Test
    void getCredentialsByToken_Expired_ThrowsException() {
        testUser.setRememberToken("token123");
        testUser.setRememberTokenExpiry(LocalDateTime.now().minusSeconds(1));
        when(authUserRepository.findByRememberToken("token123")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> authService.getCredentialsByToken("token123"));
    }

    @Test
    void getCredentialsByToken_NoExpiry_ThrowsException() {
        testUser.setRememberToken("token123");
        testUser.setRememberTokenExpiry(null);
        when(authUserRepository.findByRememberToken("token123")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> authService.getCredentialsByToken("token123"));
    }

    @Test
    void processForgotPassword_NoEmail_NoEmailSent() {
        testUser.setEmail(null);
        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

        authService.processForgotPassword("testuser");

        verify(emailService, never()).sendForgotPasswordEmail(any(), any(), any());
    }

    @Test
    void processForgotPassword_EmailFailure_LogsError() {
        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("Mail server down")).when(emailService)
                .sendForgotPasswordEmail(any(), any(), any());

        assertDoesNotThrow(() -> authService.processForgotPassword("testuser"));
    }

    @Test
    void loginWithToken_Success() {
        testUser.setRecoveryToken("token123");
        testUser.setRecoveryTokenExpiry(LocalDateTime.now().plusMinutes(10));
        when(authUserRepository.findByRecoveryToken("token123")).thenReturn(Optional.of(testUser));

        AuthUser result = authService.loginWithToken("token123");

        assertNotNull(result);
        assertNull(testUser.getRecoveryToken());
        assertNull(testUser.getRecoveryTokenExpiry());
        verify(authUserRepository).save(testUser);
    }

    @Test
    void loginWithToken_NotFound_ThrowsException() {
        when(authUserRepository.findByRecoveryToken("invalid")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.loginWithToken("invalid"));
    }

    @Test
    void loginWithToken_Expired_ThrowsException() {
        testUser.setRecoveryToken("token123");
        testUser.setRecoveryTokenExpiry(LocalDateTime.now().minusMinutes(1));
        when(authUserRepository.findByRecoveryToken("token123")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> authService.loginWithToken("token123"));
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token123");
        request.setNewPassword("newpassword");

        testUser.setRecoveryToken("token123");
        testUser.setRecoveryTokenExpiry(LocalDateTime.now().plusMinutes(10));
        when(authUserRepository.findByRecoveryToken("token123")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpassword")).thenReturn("encodednew");

        AuthUser result = authService.resetPassword(request);

        assertNotNull(result);
        assertEquals("encodednew", testUser.getPassword());
        assertNull(testUser.getRecoveryToken());
        verify(authUserRepository).save(testUser);
    }

    @Test
    void resetPassword_NotFound_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid");
        request.setNewPassword("newpassword");

        when(authUserRepository.findByRecoveryToken("invalid")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    }

    @Test
    void resetPassword_Expired_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token123");
        request.setNewPassword("newpassword");

        testUser.setRecoveryToken("token123");
        testUser.setRecoveryTokenExpiry(LocalDateTime.now().minusMinutes(1));
        when(authUserRepository.findByRecoveryToken("token123")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> authService.resetPassword(request));
    }
}
