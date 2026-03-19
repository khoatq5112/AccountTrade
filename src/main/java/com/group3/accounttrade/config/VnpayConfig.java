package com.group3.accounttrade.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Configuration class for VNPAY payment gateway integration.
 * Supports both production and sandbox environments.
 */
@Configuration
@Getter
public class VnpayConfig {

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${vnpay.return-url:}")
    private String returnUrl;

    @Value("${vnpay.ipn-url:}")
    private String ipnUrl;

    @Value("${vnpay.api-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String apiUrl;

    @Value("${vnpay.version:2.1.0}")
    private String version;

    @Value("${vnpay.timeout-minutes:15}")
    private int timeoutMinutes;

    @Value("${vnpay.sandbox-mode:true}")
    private boolean sandboxMode;

    // Response codes
    public static final String RESPONSE_SUCCESS = "00";
    public static final String RESPONSE_PENDING = "02";
    public static final String RESPONSE_FAILED = "01";
    public static final String RESPONSE_FRAUD = "07";
    public static final String RESPONSE_PROCESSING = "04";

    // Transaction status codes
    public static final String TXN_STATUS_SUCCESS = "00";
    public static final String TXN_STATUS_PENDING = "01";
    public static final String TXN_STATUS_FAILED = "02";

    // Command codes
    public static final String COMMAND_PAY = "pay";
    public static final String COMMAND_QUERY = "querydr";
    public static final String COMMAND_REFUND = "refund";

    // Currency codes
    public static final String CURRENCY_VND = "VND";
    public static final String CURRENCY_USD = "USD";

    // Locale
    public static final String LOCALE_VN = "vn";
    public static final String LOCALE_EN = "en";

    /**
     * Generates a unique transaction reference based on order number and timestamp.
     * Format: ORDER_NUMBER_TIMESTAMP_RANDOM
     */
    public String generateTxnRef(String orderNumber) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%04d", new Random().nextInt(10000));
        return String.format("%s_%s_%s", orderNumber, timestamp, random);
    }

    /**
     * Generates the secure hash for VNPAY request validation.
     * Uses HMAC-SHA512 algorithm as required by VNPAY.
     *
     * @param fields Map of all fields to be hashed
     * @return The secure hash string
     */
    public String generateSecureHash(Map<String, String> fields) {
        return generateSecureHash(fields, hashSecret);
    }

    /**
     * Generates the secure hash with a specific secret key.
     *
     * @param fields Map of all fields to be hashed
     * @param secret The secret key to use
     * @return The secure hash string
     */
    public String generateSecureHash(Map<String, String> fields, String secret) {
        // Sort the fields by key name
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        // Build the hash data string
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append("&");
                }
                hashData.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
        }

        // Generate HMAC-SHA512 hash
        return hmacSHA512(secret, hashData.toString());
    }

    /**
     * Validates the VNPAY response checksum.
     *
     * @param fields All response fields including vnp_SecureHash
     * @return true if the checksum is valid
     */
    public boolean validateChecksum(Map<String, String> fields) {
        String receivedHash = fields.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        // Create a copy without the hash field
        Map<String, String> fieldsToHash = new TreeMap<>(fields);
        fieldsToHash.remove("vnp_SecureHash");
        fieldsToHash.remove("vnp_SecureHashType");

        // Generate hash and compare
        String calculatedHash = generateSecureHash(fieldsToHash);
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    /**
     * Validates the VNPAY response checksum with specific secret.
     *
     * @param fields All response fields including vnp_SecureHash
     * @param secret The secret key to use
     * @return true if the checksum is valid
     */
    public boolean validateChecksum(Map<String, String> fields, String secret) {
        String receivedHash = fields.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        // Create a copy without the hash field
        Map<String, String> fieldsToHash = new TreeMap<>(fields);
        fieldsToHash.remove("vnp_SecureHash");
        fieldsToHash.remove("vnp_SecureHashType");

        // Generate hash and compare
        String calculatedHash = generateSecureHash(fieldsToHash, secret);
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    /**
     * Generates HMAC-SHA512 hash.
     *
     * @param key  The secret key
     * @param data The data to hash
     * @return The hex string of the hash
     */
    private String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA512 hash", e);
        }
    }

    /**
     * Converts bytes to hexadecimal string.
     *
     * @param bytes The byte array
     * @return The hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Converts amount to VNPAY format (amount * 100).
     * VNPAY requires amounts in the smallest currency unit (cents for VND means x100).
     *
     * @param amount The amount in VND
     * @return The amount in VNPAY format
     */
    public String formatAmountForVnpay(double amount) {
        return String.valueOf((long) (amount * 100));
    }

    /**
     * Converts VNPAY amount format back to regular amount.
     *
     * @param vnpAmount The amount from VNPAY (amount * 100)
     * @return The regular amount
     */
    public double parseAmountFromVnpay(String vnpAmount) {
        if (vnpAmount == null || vnpAmount.isEmpty()) {
            return 0.0;
        }
        return Long.parseLong(vnpAmount) / 100.0;
    }

    /**
     * Generates the current timestamp in VNPAY format (yyyyMMddHHmmss).
     *
     * @return The formatted timestamp
     */
    public String generateTimestamp() {
        return generateTimestamp(new Date());
    }

    /**
     * Generates a timestamp in VNPAY format.
     *
     * @param date The date to format
     * @return The formatted timestamp
     */
    public String generateTimestamp(Date date) {
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(date);
    }

    /**
     * Parses a VNPAY timestamp string to Date.
     *
     * @param timestamp The timestamp string
     * @return The parsed Date
     */
    public Date parseTimestamp(String timestamp) {
        try {
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
            return formatter.parse(timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates the expiration timestamp for payment (current time + timeout).
     *
     * @return The expiration timestamp
     */
    public String generateExpireTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, timeoutMinutes);
        return generateTimestamp(calendar.getTime());
    }

    /**
     * Gets the client IP address from request.
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    public String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // Handle multiple IPs (take the first one)
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    /**
     * Checks if the configuration is valid.
     *
     * @return true if all required configuration is present
     */
    public boolean isConfigured() {
        return tmnCode != null && !tmnCode.isEmpty()
                && hashSecret != null && !hashSecret.isEmpty()
                && returnUrl != null && !returnUrl.isEmpty()
                && ipnUrl != null && !ipnUrl.isEmpty();
    }
}
