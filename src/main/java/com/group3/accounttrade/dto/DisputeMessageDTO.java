package com.group3.accounttrade.dto;

import java.time.LocalDateTime;

/**
 * DTO for dispute messages in the message thread.
 */
public record DisputeMessageDTO(
    Long messageId,
    Long disputeId,
    Integer senderId,
    String senderUsername,
    String senderRole,
    String content,
    Boolean hasAttachment,
    String attachmentPath,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {
}
