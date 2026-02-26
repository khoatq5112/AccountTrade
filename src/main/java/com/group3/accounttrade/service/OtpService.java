package com.group3.accounttrade.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final Map<String, OtpDetails> otpStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateAndStoreOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1000000));
        OtpDetails details = new OtpDetails(otp, LocalDateTime.now().plusMinutes(10));
        otpStorage.put(email, details);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpDetails details = otpStorage.get(email);
        if (details == null) {
            return false;
        }

        if (details.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpStorage.remove(email);
            return false;
        }

        if (details.getOtp().equals(otp)) {
            otpStorage.remove(email);
            return true;
        }

        return false;
    }

    private static class OtpDetails {
        private final String otp;
        private final LocalDateTime expiryTime;

        public OtpDetails(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }
}
