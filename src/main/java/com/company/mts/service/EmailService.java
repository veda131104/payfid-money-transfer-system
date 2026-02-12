package com.company.mts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

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
        message.setText("Your OTP for account setup is: " + otp + ". This OTP is valid for 10 minutes.");

        mailSender.send(message);
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

        mailSender.send(message);
    }
}
