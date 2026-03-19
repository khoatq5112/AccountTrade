package com.group3.accounttrade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed DTO for dispute information including all related data.
 * Used for dispute detail views in buyer, seller, and admin interfaces.
 */
public record DisputeDetailDTO(
    Long disputeId,
    String disputeNumber,
    String status,
    String disputeType,
    String reason,
    String buyerEvidence,
    String sellerResponse,
    String sellerEvidence,
    String resolutionType,
    String resolutionNotes,
    String adminNotes,
    LocalDateTime openedAt,
    LocalDateTime sellerRespondedAt,
    LocalDateTime sellerResponseDeadline,
    LocalDateTime adminReviewStartedAt,
    LocalDateTime resolvedAt,
    OrderInfo order,
    UserInfo buyer,
    UserInfo seller,
    UserSummary assignedAdmin,
    UserSummary resolvedBy,
    List<DisputeMessageDTO> messages,
    List<DisputeEventDTO> timeline
) {
    
    /**
     * Order information included in dispute detail.
     */
    public record OrderInfo(
        Long orderId,
        String orderNumber,
        BigDecimal totalAmount,
        String orderStatus,
        LocalDateTime createdAt
    ) {}
    
    /**
     * User information for buyer/seller.
     */
    public record UserInfo(
        Integer userId,
        String username,
        String email
    ) {}
    
    /**
     * Summary of admin user.
     */
    public record UserSummary(
        Integer userId,
        String username
    ) {}
}
