package com.aitravel.reseller.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your OTP Code for AI Travel Reseller Network Platform");
            message.setText("Your OTP code is: " + otp + "\nThis code will expire in 5 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
            // Re-throw or handle depending on requirements. We'll throw a runtime exception.
            throw new RuntimeException("Failed to send email");
        }
    }

    public void sendPasswordResetOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Password Reset OTP - AI Travel Reseller Network");
            message.setText("Your OTP code to reset your password is: " + otp + "\nThis code will expire in 5 minutes. If you did not request this, please ignore this email.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}", to, e);
            // Re-throw or handle depending on requirements
            throw new RuntimeException("Failed to send email");
        }
    }
}
