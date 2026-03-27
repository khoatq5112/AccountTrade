package com.group3.accounttrade.util;

import com.group3.accounttrade.exception.InvalidIdException;

/**
 * Helper utility for decoding IDs with consistent error handling.
 * This class wraps IdEncoder and throws InvalidIdException for any decoding errors,
 * providing a consistent way to handle ID decoding across all controllers.
 */
public class IdDecoderHelper {

    private final IdEncoder idEncoder;

    public IdDecoderHelper(IdEncoder idEncoder) {
        this.idEncoder = idEncoder;
    }

    /**
     * Decode a post ID, throwing InvalidIdException on failure.
     *
     * @param encoded the encoded post ID
     * @return the decoded post ID
     * @throws InvalidIdException if the ID cannot be decoded
     */
    public Integer decodePostId(String encoded) {
        try {
            return idEncoder.decodePostId(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("post", encoded, "ID bài đăng không hợp lệ: " + e.getMessage(), e);
        }
    }

    /**
     * Decode an order ID, throwing InvalidIdException on failure.
     *
     * @param encoded the encoded order ID
     * @return the decoded order ID
     * @throws InvalidIdException if the ID cannot be decoded
     */
    public Long decodeOrderId(String encoded) {
        try {
            return idEncoder.decodeOrderId(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("order", encoded, "ID đơn hàng không hợp lệ: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a dispute ID, throwing InvalidIdException on failure.
     *
     * @param encoded the encoded dispute ID
     * @return the decoded dispute ID
     * @throws InvalidIdException if the ID cannot be decoded
     */
    public Long decodeDisputeId(String encoded) {
        try {
            return idEncoder.decodeDisputeId(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("dispute", encoded, "ID tranh chấp không hợp lệ: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a credential ID, throwing InvalidIdException on failure.
     *
     * @param encoded the encoded credential ID
     * @return the decoded credential ID
     * @throws InvalidIdException if the ID cannot be decoded
     */
    public Integer decodeCredentialId(String encoded) {
        try {
            return idEncoder.decodeCredentialId(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("credential", encoded, "ID tài khoản không hợp lệ: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a category ID, throwing InvalidIdException on failure.
     *
     * @param encoded the encoded category ID
     * @return the decoded category ID
     * @throws InvalidIdException if the ID cannot be decoded
     */
    public Integer decodeCategoryId(String encoded) {
        try {
            return idEncoder.decodeCategoryId(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("category", encoded, "ID danh mục không hợp lệ: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a generic Integer ID, throwing InvalidIdException on failure.
     *
     * @param encoded the encoded ID
     * @return the decoded ID
     * @throws InvalidIdException if the ID cannot be decoded
     */
    public Integer decode(String encoded) {
        try {
            return idEncoder.decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("ID", encoded, "ID không hợp lệ: " + e.getMessage(), e);
        }
    }

    /**
     * Decode a generic Long ID, throwing InvalidIdException on failure.
     *
     * @param encoded the encoded ID
     * @return the decoded ID
     * @throws InvalidIdException if the ID cannot be decoded
     */
    public Long decodeLong(String encoded) {
        try {
            return idEncoder.decodeLong(encoded);
        } catch (IllegalArgumentException e) {
            throw new InvalidIdException("ID", encoded, "ID không hợp lệ: " + e.getMessage(), e);
        }
    }
}
