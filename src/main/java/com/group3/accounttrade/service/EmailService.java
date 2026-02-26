package com.group3.accounttrade.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("TrustBridge Market - Your OTP Code");
            message.setText(
                    "Your OTP code for account activation is: " + otp + "\nThis code will expire in 10 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ". OTP is: " + otp);
            e.printStackTrace();
        }
    }
}
