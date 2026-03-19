package com.group3.accounttrade.service;

import com.group3.accounttrade.dto.DisputeDetailDTO;
import com.group3.accounttrade.dto.DisputeDTO;
import com.group3.accounttrade.dto.DisputeEventDTO;
import com.group3.accounttrade.dto.DisputeMessageDTO;
import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing dispute operations in the transaction workflow.
 * 
 * Dispute Lifecycle:
 * 1. Buyer opens dispute (status: OPENED)
 * 2. Admin reviews (status: UNDER_REVIEW)
 * 3. Resolution - either refund buyer or release to seller (status: RESOLVED)
 * 4. Or buyer cancels dispute (status: CANCELLED)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository disputeMessageRepository;
    private final DisputeStatusRepository disputeStatusRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final EscrowService escrowService;
    private final CredentialService credentialService;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final AdminReviewRepository adminReviewRepository;
    private final RefundRequestRepository refundRequestRepository;

    // Dispute status constants
    public static final String STATUS_OPENED = "OPENED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Dispute reason constants
    public static final String REASON_INVALID_CREDENTIAL = "INVALID_CREDENTIAL";
    public static final String REASON_CREDENTIAL_CHANGED = "CREDENTIAL_CHANGED";
    public static final String REASON_NOT_AS_DESCRIBED = "NOT_AS_DESCRIBED";
    public static final String REASON_NO_CREDENTIAL_RECEIVED = "NO_CREDENTIAL_RECEIVED";
    public static final String REASON_OTHER = "OTHER";

    /**
     * Opens a new dispute for an order.
     *
     * @param orderId The order ID
     * @param buyerId The buyer ID
     * @param reason The dispute reason
     * @param description Detailed description
     * @return The created dispute
     */
    @Transactional
    public Dispute openDispute(Long orderId, Integer buyerId, String reason, String description) {
        return openDispute(orderId, buyerId, reason, description, List.of());
    }

    /**
     * Opens a new dispute for an order with image evidence.
     *
     * @param orderId The order ID
     * @param buyerId The buyer ID
     * @param reason The dispute reason
     * @param description Detailed description
     * @param imageUrls List of image evidence URLs
     * @return The created dispute
     */
    @Transactional
    public Dispute openDispute(Long orderId, Integer buyerId, String reason, String description, List<String> imageUrls) {
        log.info("[DISPUTE] Starting openDispute - orderId: {}, buyerId: {}, reason: {}", orderId, buyerId, reason);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("[DISPUTE] Order not found: {}", orderId);
                    return new IllegalArgumentException("Order not found: " + orderId);
                });
        log.info("[DISPUTE] Found order: {}, buyer: {}", order.getOrderNumber(), order.getBuyer().getUsername());

        // Verify buyer owns this order
        if (!order.getBuyer().getUserId().equals(buyerId)) {
            log.error("[DISPUTE] Buyer mismatch - order buyer: {}, request buyer: {}", order.getBuyer().getUserId(), buyerId);
            throw new IllegalStateException("Only the buyer can open a dispute for this order");
        }

        // Check if order can be disputed
        String orderStatus = order.getOrderStatus().getStatusName();
        log.info("[DISPUTE] Order status: {}", orderStatus);
        if (!OrderStatus.CREDENTIAL_ASSIGNED.equals(orderStatus) &&
            !OrderStatus.AWAITING_BUYER_CONFIRMATION.equals(orderStatus)) {
            log.error("[DISPUTE] Order cannot be disputed in status: {}", orderStatus);
            throw new IllegalStateException("Order cannot be disputed in status: " + orderStatus);
        }

        // Check if dispute already exists
        List<Dispute> existingDisputes = disputeRepository.findByOrder(order);
        if (!existingDisputes.isEmpty()) {
            log.error("[DISPUTE] Dispute already exists for order: {}, existing count: {}", orderId, existingDisputes.size());
            throw new IllegalStateException("A dispute already exists for this order");
        }

        // Get opened status
        log.info("[DISPUTE] Looking up OPENED status...");
        DisputeStatus openedStatus = disputeStatusRepository.findByStatusName(STATUS_OPENED)
                .orElseThrow(() -> {
                    log.error("[DISPUTE] OPENED status not found in database!");
                    return new IllegalStateException("OPENED status not found");
                });
        log.info("[DISPUTE] Found OPENED status with ID: {}", openedStatus.getStatusId());

        // Combine description with image URLs for evidence
        String evidence = description != null ? description : "";
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String imageEvidence = "\n\n[Bằng chứng hình ảnh]:\n" + String.join("\n", imageUrls);
            evidence = evidence + imageEvidence;
            log.info("[DISPUTE] Added {} image URLs to evidence", imageUrls.size());
        }

        // Get seller from order
        log.info("[DISPUTE] Getting seller from order...");
        User seller = getSellerFromOrder(order);
        log.info("[DISPUTE] Seller: {}", seller.getUsername());

        // Generate unique dispute number
        String disputeNumber = generateDisputeNumber();
        log.info("[DISPUTE] Generated dispute number: {}", disputeNumber);

        // Create dispute
        log.info("[DISPUTE] Creating dispute entity...");
        Dispute dispute = Dispute.builder()
                .disputeNumber(disputeNumber)
                .order(order)
                .openedBy(order.getBuyer())
                .respondent(seller)
                .disputeStatus(openedStatus)
                .disputeType(reason)
                .reason(reason)
                .buyerEvidence(evidence)
                .build();
        
        log.info("[DISPUTE] Saving dispute to database...");
        disputeRepository.save(dispute);
        log.info("[DISPUTE] Dispute saved with ID: {}", dispute.getDisputeId());

        // Update order status to disputed
        log.info("[DISPUTE] Looking up DISPUTED status...");
        OrderStatus disputedStatus = orderStatusRepository.findByStatusName(OrderStatus.DISPUTED)
                .orElseThrow(() -> {
                    log.error("[DISPUTE] DISPUTED status not found in database!");
                    return new IllegalStateException("DISPUTED status not found");
                });
        order.setOrderStatus(disputedStatus);
        orderRepository.save(order);
        log.info("[DISPUTE] Order status updated to DISPUTED");

        // Freeze escrow (if exists - may not exist for older orders)
        log.info("[DISPUTE] Freezing escrow for order: {}", orderId);
        try {
            escrowService.freezeEscrow(orderId, "Dispute opened: " + reason);
            log.info("[DISPUTE] Escrow frozen successfully");
        } catch (IllegalArgumentException e) {
            // Escrow doesn't exist for this order - log warning but continue
            log.warn("[DISPUTE] No escrow found for order {}, continuing without freezing escrow: {}", orderId, e.getMessage());
        } catch (Exception e) {
            log.error("[DISPUTE] Failed to freeze escrow: {}", e.getMessage(), e);
            throw e;
        }

        // Mark credentials as disputed (if any exist)
        log.info("[DISPUTE] Marking credentials as disputed...");
        try {
            credentialService.markCredentialsAsDisputed(order, reason);
            log.info("[DISPUTE] Credentials marked as disputed");
        } catch (Exception e) {
            log.warn("[DISPUTE] Could not mark credentials as disputed (may not exist): {}", e.getMessage());
            // Don't throw - this is not critical for dispute creation
        }

        // Create audit log
        log.info("[DISPUTE] Creating audit log...");
        createAuditLog(order.getBuyer(), "DISPUTE_OPENED", "Dispute", dispute.getDisputeId(),
                String.format("Dispute opened for order %s. Reason: %s", order.getOrderNumber(), reason),
                AuditLog.ROLE_BUYER);

        // Notify seller
        log.info("[DISPUTE] Sending notifications...");
        notifySeller(order, "Dispute Opened",
                String.format("A dispute has been opened for order %s. Reason: %s", order.getOrderNumber(), reason));
        notifyBuyer(order, "Dispute Opened",
                String.format("Your dispute for order %s has been created successfully. TrustBridge will review it if buyer and seller cannot resolve it directly.",
                        order.getOrderNumber()));

        // Notify admin
        notifyAdmins("New Dispute Requires Review",
                String.format("Dispute #%d for order %s requires review.", dispute.getDisputeId(), order.getOrderNumber()));

        log.info("[DISPUTE] Dispute opened successfully - orderId: {}, disputeId: {}, reason: {}", orderId, dispute.getDisputeId(), reason);

        return dispute;
    }

    /**
     * Adds a message to a dispute.
     *
     * @param disputeId The dispute ID
     * @param userId The user ID sending the message
     * @param message The message content
     * @param attachmentUrl Optional attachment URL
     * @return The created message
     */
    @Transactional
    public DisputeMessage addMessage(Long disputeId, Integer userId, String message, String attachmentUrl) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        // Verify user is involved in dispute
        boolean isBuyer = dispute.getOpenedBy().getUserId().equals(userId);
        boolean isSeller = dispute.getRespondent().getUserId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new IllegalStateException("Only buyer or seller can add messages to this dispute");
        }

        User sender = getUserById(userId);
        DisputeMessage disputeMessage = DisputeMessage.builder()
                .dispute(dispute)
                .sender(sender)
                .senderRole(isBuyer ? DisputeMessage.ROLE_BUYER : DisputeMessage.ROLE_SELLER)
                .content(message)
                .attachmentPath(attachmentUrl)
                .hasAttachment(attachmentUrl != null && !attachmentUrl.isBlank())
                .build();
        disputeMessageRepository.save(disputeMessage);

        // Update dispute timestamp
        dispute.setUpdatedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        log.info("Message added to dispute {} by user {}", disputeId, userId);

        return disputeMessage;
    }

    /**
     * Escalates dispute for admin review.
     *
     * @param disputeId The dispute ID
     * @param userId The user escalating
     * @param reason The escalation reason
     */
    @Transactional
    public void escalateDispute(Long disputeId, Integer userId, String reason) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        DisputeStatus underReviewStatus = disputeStatusRepository.findByStatusName(STATUS_UNDER_REVIEW)
                .orElseThrow(() -> new IllegalStateException("UNDER_REVIEW status not found"));

        dispute.setDisputeStatus(underReviewStatus);
        dispute.setAdminReviewStartedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        // Create admin review record
        AdminReview adminReview = AdminReview.builder()
                .reviewType("DISPUTE")
                .entityType("Dispute")
                .entityId(disputeId)
                .issueDescription(reason)
                .status("PENDING")
                .build();
        adminReviewRepository.save(adminReview);

        // Create audit log
        createAuditLog(null, "DISPUTE_ESCALATED", "Dispute", disputeId,
                String.format("Dispute %d escalated. Reason: %s", disputeId, reason),
                AuditLog.ROLE_SYSTEM);

        // Notify admins
        notifyAdmins("Dispute Escalated",
                String.format("Dispute #%d has been escalated and requires immediate review.", disputeId));

        log.info("Dispute {} escalated for admin review", disputeId);
    }

    /**
     * Resolves dispute in buyer's favor (refund).
     *
     * @param disputeId The dispute ID
     * @param adminId The admin ID
     * @param resolution The resolution details
     * @param refundAmount The refund amount (null for full refund)
     */
    @Transactional
    public void resolveInBuyerFavor(Long disputeId, Integer adminId, String resolution, java.math.BigDecimal refundAmount) {
        log.info("[DEBUG] resolveInBuyerFavor - Starting for disputeId: {}, adminId: {}, refundAmount: {}",
                disputeId, adminId, refundAmount);
        
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> {
                    log.error("[DEBUG] Dispute not found: {}", disputeId);
                    return new IllegalArgumentException("Dispute not found: " + disputeId);
                });
        log.info("[DEBUG] Found dispute: {}", dispute.getDisputeId());

        User admin = getUserById(adminId);
        log.info("[DEBUG] Admin lookup result: {}", admin != null ? admin.getUsername() : "null");

        Order order = dispute.getOrder();
        log.info("[DEBUG] Order from dispute: {}", order != null ? order.getOrderId() : "null");
        
        if (order == null) {
            log.error("[DEBUG] Order is null for dispute: {}", disputeId);
            throw new IllegalStateException("Dispute has no associated order");
        }
        
        log.info("[DEBUG] Order ID: {}, OrderNumber: {}", order.getOrderId(), order.getOrderNumber());

        // Update dispute status
        log.info("[DEBUG] Looking up RESOLVED status...");
        DisputeStatus resolvedStatus = disputeStatusRepository.findByStatusName(STATUS_RESOLVED)
                .orElseThrow(() -> {
                    log.error("[DEBUG] RESOLVED status not found in database!");
                    return new IllegalStateException("RESOLVED status not found");
                });
        dispute.setDisputeStatus(resolvedStatus);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolutionNotes(resolution);
        dispute.setResolutionType("BUYER_FAVOR");
        dispute.setResolvedByAdmin(admin);
        disputeRepository.save(dispute);
        log.info("[DEBUG] Dispute status updated to RESOLVED");

        // Process refund
        log.info("[DEBUG] Processing escrow refund for orderId: {}", order.getOrderId());
        try {
            escrowService.refundEscrow(order.getOrderId(), refundAmount, resolution, adminId);
            log.info("[DEBUG] Escrow refund processed successfully");
        } catch (Exception e) {
            log.error("[DEBUG] Escrow refund failed: {} - {}", e.getClass().getName(), e.getMessage(), e);
            throw e;
        }

        // Create refund request record
        log.info("[DEBUG] Creating refund request record...");
        String refundNumber = generateRefundNumber();
        log.info("[DEBUG] Generated refund number: {}", refundNumber);
        
        RefundRequest refundRequest = RefundRequest.builder()
                .refundNumber(refundNumber)
                .order(order)
                .dispute(dispute)
                .requestedBy(order.getBuyer())
                .refundType(RefundRequest.TYPE_DISPUTE_RESOLUTION)
                .originalAmount(order.getTotalAmount())
                .refundAmount(refundAmount != null ? refundAmount : order.getTotalAmount())
                .reason(resolution)
                .status(RefundRequest.STATUS_APPROVED)
                .processedByAdmin(admin)
                .processedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedByAdmin(admin)
                .build();
        refundRequestRepository.save(refundRequest);
        log.info("[DEBUG] Refund request record created with number: {}", refundNumber);

        // Update order status
        log.info("[DEBUG] Updating order status to REFUNDED...");
        OrderStatus refundedStatus = orderStatusRepository.findByStatusName(OrderStatus.REFUNDED)
                .orElseThrow(() -> {
                    log.error("[DEBUG] REFUNDED status not found in database!");
                    return new IllegalStateException("REFUNDED status not found");
                });
        order.setOrderStatus(refundedStatus);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason("Dispute resolved in buyer favor: " + resolution);
        orderRepository.save(order);
        log.info("[DEBUG] Order status updated to REFUNDED");

        // Revoke credentials
        log.info("[DEBUG] Revoking credentials...");
        credentialService.revokeCredentials(order, "Dispute resolved in buyer favor");
        log.info("[DEBUG] Credentials revoked");

        // Create audit log
        createAuditLog(admin, "DISPUTE_RESOLVED_BUYER", "Dispute", disputeId,
                String.format("Dispute %d resolved in buyer favor. Refund: %s", disputeId,
                        refundAmount != null ? refundAmount : order.getTotalAmount()),
                AuditLog.ROLE_ADMIN);

        // Notify parties
        notifyBuyer(order, "Dispute Resolved",
                String.format("Your dispute for order %s has been resolved in your favor. Refund processed.",
                        order.getOrderNumber()));
        notifySeller(order, "Dispute Resolved",
                String.format("The dispute for order %s has been resolved in buyer's favor.", order.getOrderNumber()));

        log.info("[DEBUG] Dispute {} resolved in buyer favor successfully, refund: {}", disputeId, refundAmount);
    }

    /**
     * Resolves dispute in seller's favor (release escrow).
     *
     * @param disputeId The dispute ID
     * @param adminId The admin ID
     * @param resolution The resolution details
     */
    @Transactional
    public void resolveInSellerFavor(Long disputeId, Integer adminId, String resolution) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        User admin = getUserById(adminId);
        Order order = dispute.getOrder();

        // Update dispute status
        DisputeStatus resolvedStatus = disputeStatusRepository.findByStatusName(STATUS_RESOLVED)
                .orElseThrow(() -> new IllegalStateException("RESOLVED status not found"));
        dispute.setDisputeStatus(resolvedStatus);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolutionNotes(resolution);
        dispute.setResolutionType("SELLER_FAVOR");
        dispute.setResolvedByAdmin(admin);
        disputeRepository.save(dispute);

        // Unfreeze and release escrow
        escrowService.unfreezeEscrow(order.getOrderId(), "Dispute resolved in seller favor");
        escrowService.releaseEscrow(order.getOrderId(), "Dispute resolved in seller favor: " + resolution, adminId);

        // Create audit log
        createAuditLog(admin, "DISPUTE_RESOLVED_SELLER", "Dispute", disputeId,
                String.format("Dispute %d resolved in seller favor.", disputeId),
                AuditLog.ROLE_ADMIN);

        // Notify parties
        notifyBuyer(order, "Dispute Resolved",
                String.format("Your dispute for order %s has been resolved in seller's favor.", order.getOrderNumber()));
        notifySeller(order, "Dispute Resolved",
                String.format("The dispute for order %s has been resolved in your favor. Payment released.", 
                        order.getOrderNumber()));

        log.info("Dispute {} resolved in seller favor", disputeId);
    }

    /**
     * Cancels a dispute (buyer only).
     *
     * @param disputeId The dispute ID
     * @param buyerId The buyer ID
     * @param reason The cancellation reason
     */
    @Transactional
    public void cancelDispute(Long disputeId, Integer buyerId, String reason) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        // Verify buyer owns this dispute
        if (!dispute.getOpenedBy().getUserId().equals(buyerId)) {
            throw new IllegalStateException("Only the buyer can cancel this dispute");
        }

        // Update dispute status
        DisputeStatus cancelledStatus = disputeStatusRepository.findByStatusName(STATUS_CANCELLED)
                .orElseThrow(() -> new IllegalStateException("CANCELLED status not found"));
        dispute.setDisputeStatus(cancelledStatus);
        dispute.setResolutionNotes(reason);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute.setResolutionType("CANCELLED");
        disputeRepository.save(dispute);

        Order order = dispute.getOrder();

        // Unfreeze escrow
        escrowService.unfreezeEscrow(order.getOrderId(), "Dispute cancelled by buyer");

        // Restore order status
        OrderStatus awaitingStatus = orderStatusRepository.findByStatusName(OrderStatus.AWAITING_BUYER_CONFIRMATION)
                .orElseThrow(() -> new IllegalStateException("AWAITING_BUYER_CONFIRMATION status not found"));
        order.setOrderStatus(awaitingStatus);
        orderRepository.save(order);

        // Create audit log
        createAuditLog(dispute.getOpenedBy(), "DISPUTE_CANCELLED", "Dispute", disputeId,
                String.format("Dispute %d cancelled by buyer. Reason: %s", disputeId, reason),
                AuditLog.ROLE_BUYER);

        // Notify seller
        notifySeller(order, "Dispute Cancelled",
                String.format("The dispute for order %s has been cancelled by the buyer.", order.getOrderNumber()));

        log.info("Dispute {} cancelled by buyer", disputeId);
    }

    /**
     * Gets all disputes for a buyer.
     *
     * @param buyerId The buyer ID
     * @return List of disputes
     */
    public List<Dispute> getDisputesByBuyer(Integer buyerId) {
        User buyer = getUserById(buyerId);
        return buyer == null ? List.of() : disputeRepository.findByOpenedByOrderByOpenedAtDesc(buyer);
    }

    /**
     * Gets all disputes for a seller.
     *
     * @param sellerId The seller ID
     * @return List of disputes
     */
    public List<Dispute> getDisputesBySeller(Integer sellerId) {
        User seller = getUserById(sellerId);
        return seller == null ? List.of() : disputeRepository.findByRespondentOrderByOpenedAtDesc(seller);
    }

    /**
     * Gets all open disputes (for admin).
     *
     * @return List of open disputes
     */
    public List<Dispute> getOpenDisputes() {
        DisputeStatus openedStatus = disputeStatusRepository.findByStatusName(STATUS_OPENED)
                .orElse(null);
        DisputeStatus underReviewStatus = disputeStatusRepository.findByStatusName(STATUS_UNDER_REVIEW)
                .orElse(null);
        
        List<Dispute> disputes = new java.util.ArrayList<>();
        if (openedStatus != null) {
            disputes.addAll(disputeRepository.findByDisputeStatus(openedStatus));
        }
        if (underReviewStatus != null) {
            disputes.addAll(disputeRepository.findByDisputeStatus(underReviewStatus));
        }
        return disputes;
    }

    /**
     * Gets messages for a dispute.
     *
     * @param disputeId The dispute ID
     * @return List of messages
     */
    public List<DisputeMessage> getDisputeMessages(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .map(disputeMessageRepository::findByDisputeOrderByCreatedAtAsc)
                .orElse(List.of());
    }

    /**
     * Gets a dispute by ID.
     *
     * @param disputeId The dispute ID
     * @return The dispute if found
     */
    public Optional<Dispute> getDisputeById(Long disputeId) {
        return disputeRepository.findById(disputeId);
    }

    /**
     * Gets dispute for an order.
     *
     * @param orderId The order ID
     * @return The dispute if exists
     */
    public Optional<Dispute> getDisputeByOrderId(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> disputeRepository.findByOrder(order).stream().findFirst());
    }

    /**
     * Extracts seller from order (assumes single seller per order).
     */
    private User getSellerFromOrder(Order order) {
        return order.getOrderItems().stream()
                .findFirst()
                .map(item -> item.getPost().getSeller())
                .orElseThrow(() -> new IllegalStateException("Order has no items"));
    }

    /**
     * Gets user by ID.
     */
    private User getUserById(Integer userId) {
        return userRepository != null ? userRepository.findById(userId).orElse(null) : null;
    }

    private final UserRepository userRepository;

    /**
     * Creates an audit log entry.
     */
    private void createAuditLog(User user, String action, String entityType,
            Long entityId, String description, String performerRole) {
        
        AuditLog auditLog = AuditLog.builder()
                .eventType(AuditLog.EVENT_DISPUTE)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(user)
                .performerRole(performerRole)
                .description(description)
                .success(true)
                .build();

        auditLogRepository.save(auditLog);
    }

    /**
     * Notifies the buyer.
     */
    private void notifyBuyer(Order order, String title, String message) {
        Notification notification = Notification.builder()
                .user(order.getBuyer())
                .notificationType(Notification.TYPE_DISPUTE)
                .title(title)
                .message(message)
                .relatedEntityType("ORDER")
                .relatedEntityId(order.getOrderId())
                .priority(Notification.PRIORITY_HIGH)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Notifies the seller.
     */
    private void notifySeller(Order order, String title, String message) {
        User seller = getSellerFromOrder(order);
        Notification notification = Notification.builder()
                .user(seller)
                .notificationType(Notification.TYPE_DISPUTE)
                .title(title)
                .message(message)
                .relatedEntityType("ORDER")
                .relatedEntityId(order.getOrderId())
                .priority(Notification.PRIORITY_HIGH)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Notifies admins.
     */
    private void notifyAdmins(String title, String message) {
        // Create system notification for admin review queue
        log.info("Admin notification: {} - {}", title, message);
    }

    // ==================== NEW METHODS FOR DISPUTE FUNCTIONALITY ====================

    /**
     * Gets paginated disputes for a buyer with optional status filter.
     *
     * @param buyerId The buyer ID
     * @param status Optional status filter (null for all)
     * @param pageable Pagination parameters
     * @return Page of disputes
     */
    public Page<Dispute> getDisputesByBuyerPaginated(Integer buyerId, String status, Pageable pageable) {
        User buyer = getUserById(buyerId);
        if (buyer == null) {
            return Page.empty(pageable);
        }
        return disputeRepository.findByBuyerWithStatus(buyer, status, pageable);
    }

    /**
     * Gets paginated disputes for a seller with optional status filter.
     *
     * @param sellerId The seller ID
     * @param status Optional status filter (null for all)
     * @param pageable Pagination parameters
     * @return Page of disputes
     */
    public Page<Dispute> getDisputesBySellerPaginated(Integer sellerId, String status, Pageable pageable) {
        User seller = getUserById(sellerId);
        if (seller == null) {
            return Page.empty(pageable);
        }
        return disputeRepository.findBySellerWithStatus(seller, status, pageable);
    }

    /**
     * Gets all disputes for admin with optional status filter.
     *
     * @param status Optional status filter (null for all)
     * @param pageable Pagination parameters
     * @return Page of DisputeDTO
     */
    public Page<DisputeDTO> getAllDisputesPaginated(String status, Pageable pageable) {
        Page<Dispute> disputesPage = disputeRepository.findAllWithStatus(status, pageable);
        return disputesPage.map(this::mapToDisputeDTO);
    }

    private DisputeDTO mapToDisputeDTO(Dispute dispute) {
        Order order = dispute.getOrder();
        return new DisputeDTO(
                dispute.getDisputeId(),
                dispute.getDisputeNumber(),
                order != null ? order.getOrderNumber() : "N/A",
                order != null ? order.getTotalAmount() : BigDecimal.ZERO,
                dispute.getReason(),
                dispute.getOpenedBy() != null ? dispute.getOpenedBy().getUsername() : "N/A",
                dispute.getRespondent() != null ? dispute.getRespondent().getUsername() : "N/A",
                dispute.getOpenedAt(),
                dispute.getDisputeStatus() != null ? dispute.getDisputeStatus().getStatusName() : "N/A",
                dispute.getDisputeType()
        );
    }

    /**
     * Gets detailed dispute information as DTO.
     *
     * @param disputeId The dispute ID
     * @return DisputeDetailDTO or null if not found
     */
    public DisputeDetailDTO getDisputeDetailDTO(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .map(this::mapToDetailDTO)
                .orElse(null);
    }

    /**
     * Submits seller response to a dispute.
     *
     * @param disputeId The dispute ID
     * @param sellerId The seller ID
     * @param response The response text
     * @param evidence Optional evidence
     */
    @Transactional
    public void submitSellerResponse(Long disputeId, Integer sellerId, String response, String evidence) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        // Verify seller is the respondent
        if (!dispute.getRespondent().getUserId().equals(sellerId)) {
            throw new IllegalStateException("Only the seller can respond to this dispute");
        }

        // Check dispute is in correct status
        if (!STATUS_OPENED.equals(dispute.getDisputeStatus().getStatusName())) {
            throw new IllegalStateException("Dispute is not in OPENED status");
        }

        // Update dispute with seller response
        dispute.setSellerResponse(response);
        dispute.setSellerEvidence(evidence);
        dispute.setSellerRespondedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        // Create audit log
        createAuditLog(dispute.getRespondent(), "SELLER_RESPONDED", "Dispute", disputeId,
                String.format("Seller responded to dispute %d", disputeId),
                AuditLog.ROLE_SELLER);

        // Notify buyer
        notifyBuyer(dispute.getOrder(), "Seller Responded",
                String.format("The seller has responded to your dispute for order %s.", 
                        dispute.getOrder().getOrderNumber()));

        log.info("Seller {} responded to dispute {}", sellerId, disputeId);
    }

    /**
     * Assigns an admin to a dispute.
     *
     * @param disputeId The dispute ID
     * @param adminId The admin ID
     */
    @Transactional
    public void assignAdmin(Long disputeId, Integer adminId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        User admin = getUserById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found: " + adminId);
        }

        dispute.setAssignedAdmin(admin);
        disputeRepository.save(dispute);

        // Create audit log
        createAuditLog(admin, "ADMIN_ASSIGNED", "Dispute", disputeId,
                String.format("Admin %s assigned to dispute %d", admin.getUsername(), disputeId),
                AuditLog.ROLE_ADMIN);

        log.info("Admin {} assigned to dispute {}", adminId, disputeId);
    }

    /**
     * Starts admin review of a dispute (changes status to UNDER_REVIEW).
     *
     * @param disputeId The dispute ID
     * @param adminId The admin ID starting the review
     */
    @Transactional
    public void startReview(Long disputeId, Integer adminId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        // Verify status is OPENED
        if (!STATUS_OPENED.equals(dispute.getDisputeStatus().getStatusName())) {
            throw new IllegalStateException("Dispute must be in OPENED status to start review");
        }

        User admin = getUserById(adminId);

        // Update status to UNDER_REVIEW
        DisputeStatus underReviewStatus = disputeStatusRepository.findByStatusName(STATUS_UNDER_REVIEW)
                .orElseThrow(() -> new IllegalStateException("UNDER_REVIEW status not found"));

        dispute.setDisputeStatus(underReviewStatus);
        dispute.setAdminReviewStartedAt(LocalDateTime.now());
        if (admin != null) {
            dispute.setAssignedAdmin(admin);
        }
        disputeRepository.save(dispute);

        // Create audit log
        createAuditLog(admin, "REVIEW_STARTED", "Dispute", disputeId,
                String.format("Admin review started for dispute %d", disputeId),
                AuditLog.ROLE_ADMIN);

        // Notify both parties
        notifyBuyer(dispute.getOrder(), "Dispute Under Review",
                String.format("Your dispute for order %s is now under admin review.", 
                        dispute.getOrder().getOrderNumber()));
        notifySeller(dispute.getOrder(), "Dispute Under Review",
                String.format("The dispute for order %s is now under admin review.", 
                        dispute.getOrder().getOrderNumber()));

        log.info("Admin review started for dispute {} by admin {}", disputeId, adminId);
    }

    /**
     * Adds internal admin notes to a dispute.
     *
     * @param disputeId The dispute ID
     * @param adminId The admin ID
     * @param notes The notes to add
     */
    @Transactional
    public void addAdminNote(Long disputeId, Integer adminId, String notes) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        User admin = getUserById(adminId);

        String existingNotes = dispute.getAdminNotes();
        String newNotes = existingNotes != null && !existingNotes.isBlank()
                ? existingNotes + "\n[" + LocalDateTime.now() + "] " + notes
                : "[" + LocalDateTime.now() + "] " + notes;
        
        dispute.setAdminNotes(newNotes);
        disputeRepository.save(dispute);

        // Create audit log
        createAuditLog(admin, "ADMIN_NOTE_ADDED", "Dispute", disputeId,
                String.format("Admin note added to dispute %d", disputeId),
                AuditLog.ROLE_ADMIN);

        log.info("Admin note added to dispute {} by admin {}", disputeId, adminId);
    }

    /**
     * Gets dispute timeline events.
     *
     * @param disputeId The dispute ID
     * @return List of timeline events
     */
    public List<DisputeEventDTO> getDisputeTimeline(Long disputeId) {
        List<DisputeEventDTO> timeline = new ArrayList<>();
        
        Dispute dispute = disputeRepository.findById(disputeId).orElse(null);
        if (dispute == null) {
            return timeline;
        }

        long eventId = 1;

        // Dispute opened
        timeline.add(new DisputeEventDTO(
                eventId++,
                DisputeEventDTO.TYPE_DISPUTE_OPENED,
                String.format("Dispute opened. Reason: %s", dispute.getReason()),
                AuditLog.ROLE_BUYER,
                dispute.getOpenedBy().getUsername(),
                dispute.getOpenedAt()
        ));

        // Seller responded
        if (dispute.getSellerRespondedAt() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_SELLER_RESPONDED,
                    String.format("Seller responded: %s", 
                            truncate(dispute.getSellerResponse(), 100)),
                    AuditLog.ROLE_SELLER,
                    dispute.getRespondent().getUsername(),
                    dispute.getSellerRespondedAt()
            ));
        }

        // Admin assigned
        if (dispute.getAssignedAdmin() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_ADMIN_ASSIGNED,
                    String.format("Admin %s assigned", dispute.getAssignedAdmin().getUsername()),
                    AuditLog.ROLE_ADMIN,
                    dispute.getAssignedAdmin().getUsername(),
                    dispute.getAdminReviewStartedAt() != null ? dispute.getAdminReviewStartedAt() : dispute.getOpenedAt()
            ));
        }

        // Review started
        if (dispute.getAdminReviewStartedAt() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_REVIEW_STARTED,
                    "Admin review started",
                    AuditLog.ROLE_ADMIN,
                    dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getUsername() : "System",
                    dispute.getAdminReviewStartedAt()
            ));
        }

        // Resolved
        if (dispute.getResolvedAt() != null) {
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    DisputeEventDTO.TYPE_DISPUTE_RESOLVED,
                    String.format("Dispute resolved: %s - %s", 
                            dispute.getResolutionType(),
                            truncate(dispute.getResolutionNotes(), 100)),
                    AuditLog.ROLE_ADMIN,
                    dispute.getResolvedByAdmin() != null ? dispute.getResolvedByAdmin().getUsername() : "System",
                    dispute.getResolvedAt()
            ));
        }

        // Add messages to timeline
        List<DisputeMessage> messages = disputeMessageRepository.findByDisputeOrderByCreatedAtAsc(dispute);
        for (DisputeMessage msg : messages) {
            String eventType = DisputeMessage.ROLE_BUYER.equals(msg.getSenderRole()) 
                    ? DisputeEventDTO.TYPE_BUYER_MESSAGE 
                    : DisputeMessage.ROLE_SELLER.equals(msg.getSenderRole())
                            ? DisputeEventDTO.TYPE_SELLER_MESSAGE
                            : DisputeEventDTO.TYPE_ADMIN_NOTE;
            
            timeline.add(new DisputeEventDTO(
                    eventId++,
                    eventType,
                    truncate(msg.getContent(), 100),
                    msg.getSenderRole(),
                    msg.getSender().getUsername(),
                    msg.getCreatedAt()
            ));
        }

        // Sort by timestamp
        timeline.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));

        return timeline;
    }

    /**
     * Maps Dispute entity to DisputeDetailDTO.
     */
    private DisputeDetailDTO mapToDetailDTO(Dispute dispute) {
        Order order = dispute.getOrder();
        
        DisputeDetailDTO.OrderInfo orderInfo = new DisputeDetailDTO.OrderInfo(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getOrderStatus().getStatusName(),
                order.getCreatedAt()
        );

        DisputeDetailDTO.UserInfo buyerInfo = new DisputeDetailDTO.UserInfo(
                dispute.getOpenedBy().getUserId(),
                dispute.getOpenedBy().getUsername(),
                dispute.getOpenedBy().getEmail()
        );

        DisputeDetailDTO.UserInfo sellerInfo = new DisputeDetailDTO.UserInfo(
                dispute.getRespondent().getUserId(),
                dispute.getRespondent().getUsername(),
                dispute.getRespondent().getEmail()
        );

        DisputeDetailDTO.UserSummary assignedAdmin = dispute.getAssignedAdmin() != null
                ? new DisputeDetailDTO.UserSummary(
                        dispute.getAssignedAdmin().getUserId(),
                        dispute.getAssignedAdmin().getUsername())
                : null;

        DisputeDetailDTO.UserSummary resolvedBy = dispute.getResolvedByAdmin() != null
                ? new DisputeDetailDTO.UserSummary(
                        dispute.getResolvedByAdmin().getUserId(),
                        dispute.getResolvedByAdmin().getUsername())
                : null;

        // Map messages
        List<DisputeMessage> messages = disputeMessageRepository.findByDisputeOrderByCreatedAtAsc(dispute);
        List<DisputeMessageDTO> messageDTOs = messages.stream()
                .map(msg -> new DisputeMessageDTO(
                        msg.getMessageId(),
                        dispute.getDisputeId(),
                        msg.getSender().getUserId(),
                        msg.getSender().getUsername(),
                        msg.getSenderRole(),
                        msg.getContent(),
                        msg.getHasAttachment(),
                        msg.getAttachmentPath(),
                        msg.getCreatedAt(),
                        msg.getReadAt()
                ))
                .toList();

        // Get timeline
        List<DisputeEventDTO> timeline = getDisputeTimeline(dispute.getDisputeId());

        return new DisputeDetailDTO(
                dispute.getDisputeId(),
                dispute.getDisputeNumber(),
                dispute.getDisputeStatus().getStatusName(),
                dispute.getDisputeType(),
                dispute.getReason(),
                dispute.getBuyerEvidence(),
                dispute.getSellerResponse(),
                dispute.getSellerEvidence(),
                dispute.getResolutionType(),
                dispute.getResolutionNotes(),
                dispute.getAdminNotes(),
                dispute.getOpenedAt(),
                dispute.getSellerRespondedAt(),
                dispute.getSellerResponseDeadline(),
                dispute.getAdminReviewStartedAt(),
                dispute.getResolvedAt(),
                orderInfo,
                buyerInfo,
                sellerInfo,
                assignedAdmin,
                resolvedBy,
                messageDTOs,
                timeline
        );
    }

    /**
     * Truncates a string to a maximum length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    /**
     * Generates a unique dispute number.
     * Format: DSP-YYYYMMDD-XXXXX
     */
    private String generateDisputeNumber() {
        String datePart = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                .format(java.time.LocalDate.now());
        long count = disputeRepository.count() + 1;
        return String.format("DSP-%s-%05d", datePart, count);
    }

    /**
     * Generates a unique refund number.
     * Format: RFD-YYYYMMDD-XXXXX
     */
    private String generateRefundNumber() {
        String datePart = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                .format(java.time.LocalDate.now());
        long count = refundRequestRepository.count() + 1;
        return String.format("RFD-%s-%05d", datePart, count);
    }

    /**
     * Gets dispute statistics for dashboard.
     *
     * @return DisputeStats object
     */
    public DisputeStats getDisputeStats() {
        long total = disputeRepository.count();
        long opened = disputeRepository.countByStatusNameIn(List.of(STATUS_OPENED));
        long underReview = disputeRepository.countByStatusNameIn(List.of(STATUS_UNDER_REVIEW));
        long resolved = disputeRepository.countByStatusNameIn(List.of(STATUS_RESOLVED));
        
        return new DisputeStats(total, opened, underReview, resolved);
    }

    /**
     * Record for dispute statistics.
     */
    public record DisputeStats(long total, long opened, long underReview, long resolved) {}
}
