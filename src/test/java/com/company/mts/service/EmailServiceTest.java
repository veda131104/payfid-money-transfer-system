package com.company.mts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "support@payfid.com");
    }

    @Test
    void sendOtpEmail_Success() {
        emailService.sendOtpEmail("user@test.com", "123456");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendForgotPasswordEmail_Success() {
        emailService.sendForgotPasswordEmail("user@test.com", "User", "token123");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOtpEmail_Failure_ThrowsException() {
        doThrow(new RuntimeException("Mail sender error")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThrows(RuntimeException.class, () -> emailService.sendOtpEmail("user@test.com", "123456"));
    }

    @Test
    void sendForgotPasswordEmail_Failure_ThrowsException() {
        doThrow(new RuntimeException("Mail sender error")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThrows(RuntimeException.class, () -> emailService.sendForgotPasswordEmail("user@test.com", "User", "token123"));
    }
}
