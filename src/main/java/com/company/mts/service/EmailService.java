package com.company.mts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Your Money Transfer System OTP");
        // message.setText("Your OTP for account setup is: " + otp + ". This OTP is valid for 10 minutes.");

        try {
            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    public void sendForgotPasswordEmail(String to, String username, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("PayFid Dashboard Magic Access");
        String link = "http://localhost:4200/reset-password?token=" + token;
        message.setText("Hello " + username + ",\n\n" +
                "You can access your PayFid dashboard directly using this link (expires in 15 minutes):\n" +
                link + "\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "PayFid Support Team");

        try {
            mailSender.send(message);
            logger.info("Forgot password email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send forgot password email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
}
