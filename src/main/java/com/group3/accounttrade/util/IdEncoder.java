package com.group3.accounttrade.util;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reversible ID obfuscation utility used for URL-safe public IDs.
 * Supports both encoded tokens and legacy numeric IDs for backward compatibility.
 */
@Component
public class IdEncoder {

    private static final String POST_PREFIX = "post_";
    private static final String ORDER_PREFIX = "order_";
    private static final String DISPUTE_PREFIX = "dispute_";
    private static final String CREDENTIAL_PREFIX = "cred_";
    private static final String CATEGORY_PREFIX = "cat_";

    private final Hashids hashids;

    public IdEncoder(
            @Value("${app.id-encoder.salt:trustbridge-default-salt}") String salt,
            @Value("${app.id-encoder.min-hash-length:8}") int minHashLength) {
        this.hashids = new Hashids(salt, minHashLength);
    }

    public String encode(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return hashids.encode(id.longValue());
    }

    public String encode(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return hashids.encode(id);
    }

    public Integer decode(String encoded) {
        long value = decodeToLongValue(encoded);
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Decoded value out of Integer range");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Decoded value must be positive");
        }
        return (int) value;
    }

    public Long decodeLong(String encoded) {
        long value = decodeToLongValue(encoded);
        if (value < 0) {
            throw new IllegalArgumentException("Decoded value must be positive");
        }
        return value;
    }

    public String encodePostId(Integer postId) {
        return POST_PREFIX + encode(postId);
    }

    public Integer decodePostId(String encoded) {
        return decodeIntegerWithPrefixOrNumeric(encoded, POST_PREFIX);
    }

    public String encodeOrderId(Long orderId) {
        return ORDER_PREFIX + encode(orderId);
    }

    public Long decodeOrderId(String encoded) {
        return decodeLongWithPrefixOrNumeric(encoded, ORDER_PREFIX);
    }

    public String encodeDisputeId(Long disputeId) {
        return DISPUTE_PREFIX + encode(disputeId);
    }

    public Long decodeDisputeId(String encoded) {
        return decodeLongWithPrefixOrNumeric(encoded, DISPUTE_PREFIX);
    }

    public String encodeCredentialId(Integer credentialId) {
        return CREDENTIAL_PREFIX + encode(credentialId);
    }

    public Integer decodeCredentialId(String encoded) {
        return decodeIntegerWithPrefixOrNumeric(encoded, CREDENTIAL_PREFIX);
    }

    public String encodeCategoryId(Integer categoryId) {
        return CATEGORY_PREFIX + encode(categoryId);
    }

    public Integer decodeCategoryId(String encoded) {
        return decodeIntegerWithPrefixOrNumeric(encoded, CATEGORY_PREFIX);
    }

    private long decodeToLongValue(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("Encoded string cannot be null or empty");
        }

        long[] decoded = hashids.decode(encoded.trim());
        if (decoded.length == 0) {
            throw new IllegalArgumentException("Invalid encoded ID: " + encoded);
        }
        return decoded[0];
    }

    private Integer decodeIntegerWithPrefixOrNumeric(String raw, String prefix) {
        String normalized = normalizeRawToken(raw, prefix);
        if (isNumeric(normalized)) {
            return Integer.parseInt(normalized);
        }
        return decode(normalized);
    }

    private Long decodeLongWithPrefixOrNumeric(String raw, String prefix) {
        String normalized = normalizeRawToken(raw, prefix);
        if (isNumeric(normalized)) {
            return Long.parseLong(normalized);
        }
        return decodeLong(normalized);
    }

    private String normalizeRawToken(String raw, String prefix) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("ID token cannot be null or empty");
        }
        String token = raw.trim();
        return token.startsWith(prefix) ? token.substring(prefix.length()) : token;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
