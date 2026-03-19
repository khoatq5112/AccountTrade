package com.group3.accounttrade.dto;

import java.time.LocalDateTime;

/**
 * DTO for dispute timeline events.
 * Represents a single event in the dispute lifecycle.
 */
public record DisputeEventDTO(
    Long eventId,
    String eventType,
    String description,
    String performerRole,
    String performerUsername,
    LocalDateTime timestamp
) {
    
    // Event type constants
    public static final String TYPE_DISPUTE_OPENED = "DISPUTE_OPENED";
    public static final String TYPE_SELLER_RESPONDED = "SELLER_RESPONDED";
    public static final String TYPE_BUYER_MESSAGE = "BUYER_MESSAGE";
    public static final String TYPE_SELLER_MESSAGE = "SELLER_MESSAGE";
    public static final String TYPE_ADMIN_ASSIGNED = "ADMIN_ASSIGNED";
    public static final String TYPE_REVIEW_STARTED = "REVIEW_STARTED";
    public static final String TYPE_ADMIN_NOTE = "ADMIN_NOTE";
    public static final String TYPE_DISPUTE_RESOLVED = "DISPUTE_RESOLVED";
    public static final String TYPE_DISPUTE_CANCELLED = "DISPUTE_CANCELLED";
}
