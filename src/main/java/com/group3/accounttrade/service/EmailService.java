package com.group3.accounttrade.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Void> sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("TrustBridge Market - Mã xác thực OTP");
            message.setText(
                    "Mã OTP của bạn là: " + otp + "\nMã này sẽ hết hạn sau 10 phút.\n\n-- TrustBridge Market");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Không thể gửi email tới " + to + ". OTP là: " + otp);
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }
}
