package com.group3.accounttrade.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
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
            log.error("Không thể gửi email OTP tới {}", to, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Không thể gửi email notification tới {}", to, e);
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }
}
