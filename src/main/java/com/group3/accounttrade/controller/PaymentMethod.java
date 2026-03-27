package com.group3.accounttrade.controller;

import java.util.Locale;

public enum PaymentMethod {
    VNPAY,
    WALLET;

    public static PaymentMethod from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return VNPAY;
        }

        try {
            return PaymentMethod.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
    }
}
