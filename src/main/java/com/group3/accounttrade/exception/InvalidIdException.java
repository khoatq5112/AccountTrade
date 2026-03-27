package com.group3.accounttrade.exception;

/**
 * Exception thrown when an ID cannot be decoded or is invalid.
 * This is used for consistent error handling across all controllers
 * when dealing with ID decoding errors.
 */
public class InvalidIdException extends RuntimeException {

    private final String idType;
    private final String idValue;

    public InvalidIdException(String message) {
        super(message);
        this.idType = null;
        this.idValue = null;
    }

    public InvalidIdException(String idType, String idValue, String message) {
        super(message);
        this.idType = idType;
        this.idValue = idValue;
    }

    public InvalidIdException(String message, Throwable cause) {
        super(message, cause);
        this.idType = null;
        this.idValue = null;
    }

    public InvalidIdException(String idType, String idValue, String message, Throwable cause) {
        super(message, cause);
        this.idType = idType;
        this.idValue = idValue;
    }

    public String getIdType() {
        return idType;
    }

    public String getIdValue() {
        return idValue;
    }
}
