package com.company.mts.service;

import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.SignupRequest;
import com.company.mts.entity.AuthUser;
import com.company.mts.exception.DuplicateUserException;
import com.company.mts.repository.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        when(authUserRepository.existsByNameIgnoreCase("testuser")).thenReturn(false);
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

        when(authUserRepository.existsByNameIgnoreCase("testuser")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.signup(request));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setName("testuser");
        request.setPassword("password123");

        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

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

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void loginWithRememberMe_SetsToken() {
        LoginRequest request = new LoginRequest();
        request.setName("testuser");
        request.setPassword("password123");
        request.setRememberMe(true);

        when(authUserRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

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
}
