package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import com.group3.accounttrade.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for managing credential operations in the transaction workflow.
 * 
 * Credential Lifecycle:
 * 1. Seller creates credentials for a post (status: AVAILABLE)
 * 2. When order is placed, credentials are reserved via CredentialAssignment
 * 3. After payment, credentials are assigned to buyer (status: ASSIGNED)
 * 4. After buyer confirmation, credentials are marked sold (status: SOLD)
 * 5. If dispute occurs, credentials may be marked disputed (status: DISPUTED)
 * 6. If credentials are invalid, they are marked invalid (status: INVALID)
 * 7. Seller can provide replacement credentials (status: REPLACED on original)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialService {

    private final PostCredentialRepository postCredentialRepository;
    private final CredentialStatusRepository credentialStatusRepository;
    private final CredentialAssignmentRepository credentialAssignmentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final PostService postService;

    // Credential status constants (matching existing CredentialStatus entity)
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_HOLDING = "Holding";
    public static final String STATUS_SOLD = "Sold";
    public static final String STATUS_HIDDEN = "Hidden";

    // Assignment status constants
    public static final String ASSIGNMENT_ASSIGNED = "ASSIGNED";
    public static final String ASSIGNMENT_DELIVERED = "DELIVERED";
    public static final String ASSIGNMENT_VIEWED = "VIEWED";
    public static final String ASSIGNMENT_CONFIRMED = "CONFIRMED";
    public static final String ASSIGNMENT_DISPUTED = "DISPUTED";
    public static final String ASSIGNMENT_REPLACED = "REPLACED";
    public static final String ASSIGNMENT_REVOKED = "REVOKED";

    /**
     * Reserves available credentials for an order item.
     * Called during checkout process before payment.
     *
     * @param post The post to reserve credentials from
     * @param quantity The number of credentials to reserve
     * @param orderItem The order item being placed
     * @return List of reserved credentials
     * @throws IllegalStateException if not enough credentials available
     */
    @Transactional
    public List<PostCredential> reserveCredentials(Post post, int quantity, OrderItem orderItem) {
        // Get available credentials for this post
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(STATUS_AVAILABLE)
                .orElseThrow(() -> new IllegalStateException("Available status not found"));

        List<PostCredential> availableCredentials = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Optional<PostCredential> credentialOpt = postCredentialRepository
                    .findFirstByPost_PostIdAndCredentialStatusOrderByCreatedAtAsc(post.getPostId(), availableStatus);
            if (credentialOpt.isEmpty()) {
                // Release any already reserved credentials
                releaseCredentialsReservation(availableCredentials);
                throw new IllegalStateException(
                        String.format("Not enough credentials available. Requested: %d, Available: %d",
                                quantity, i));
            }
            availableCredentials.add(credentialOpt.get());
        }

        // Get holding status
        CredentialStatus holdingStatus = credentialStatusRepository.findByStatusName(STATUS_HOLDING)
                .orElseThrow(() -> new IllegalStateException("Holding status not found"));

        // Mark credentials as holding
        for (PostCredential credential : availableCredentials) {
            credential.setCredentialStatus(holdingStatus);
            postCredentialRepository.save(credential);
        }

        syncPostStockStatus(post);

        // Create audit log
        createAuditLog(orderItem.getOrder().getBuyer(), "CREDENTIALS_RESERVED", "Post", post.getPostId().longValue(),
                String.format("Reserved %d credentials for post %s, order %s",
                        quantity, post.getTitle(), orderItem.getOrder().getOrderNumber()),
                AuditLog.ROLE_BUYER);

        log.info("Reserved {} credentials for post: {}, order: {}", quantity, post.getPostId(), orderItem.getOrder().getOrderNumber());

        return availableCredentials;
    }

    /**
     * Assigns reserved credentials to buyer after successful payment.
     * Creates CredentialAssignment records for tracking.
     *
     * @param order The paid order
     * @return List of credential assignments
     */
    @Transactional
    public List<CredentialAssignment> assignCredentialsToBuyer(Order order) {
        List<CredentialAssignment> assignments = new ArrayList<>();
        OrderStatus processingStatus = order.getOrderStatus();
        OrderStatus credentialAssignedStatus = order.getOrderStatus();
        OrderStatus awaitingBuyerConfirmationStatus = order.getOrderStatus();

        try {
            processingStatus = orderStatusRepository.findByStatusName(OrderStatus.PROCESSING)
                    .orElse(processingStatus);
            credentialAssignedStatus = orderStatusRepository.findByStatusName(OrderStatus.CREDENTIAL_ASSIGNED)
                    .orElse(credentialAssignedStatus);
            awaitingBuyerConfirmationStatus = orderStatusRepository
                    .findByStatusName(OrderStatus.AWAITING_BUYER_CONFIRMATION)
                    .orElse(awaitingBuyerConfirmationStatus);
        } catch (Exception ignored) {
            // Keep the current order status if the new workflow statuses are not available yet.
        }

        order.setOrderStatus(processingStatus);
        orderRepository.save(order);

        for (OrderItem item : order.getOrderItems()) {
            PostCredential credential = item.getAssignedCredential();
            if (credential == null) {
                throw new IllegalStateException("Reserved credential not found for order item " + item.getOrderItemId());
            }

            // Create assignment record from the credential reserved during checkout.
            CredentialAssignment assignment = CredentialAssignment.builder()
                    .orderItem(item)
                    .credential(credential)
                    .assignmentStatus(ASSIGNMENT_ASSIGNED)
                    .deliveredAt(LocalDateTime.now())
                    .build();
            credentialAssignmentRepository.save(assignment);
            assignments.add(assignment);

            item.setItemStatus(OrderItem.STATUS_DELIVERED);
            item.setCredentialAssignedAt(LocalDateTime.now());
            orderItemRepository.save(item);
        }

        // Update order timestamp
        order.setCredentialAssignedAt(LocalDateTime.now());
        order.setConfirmationDeadline(LocalDateTime.now().plusHours(24));
        order.setOrderStatus(credentialAssignedStatus);
        orderRepository.save(order);

        order.setOrderStatus(awaitingBuyerConfirmationStatus);
        orderRepository.save(order);

        // Create audit log
        createAuditLog(null, "CREDENTIALS_ASSIGNED", "Order", order.getOrderId(),
                String.format("Assigned %d credentials to buyer for order %s",
                        assignments.size(), order.getOrderNumber()),
                AuditLog.ROLE_SYSTEM);

        // Notify buyer
        notifyBuyer(order, "Credentials Ready",
                String.format("Your credentials for order %s are ready. Please verify and confirm receipt.",
                        order.getOrderNumber()));

        log.info("Assigned {} credentials to buyer for order: {}", assignments.size(), order.getOrderNumber());

        return assignments;
    }

    /**
     * Marks credentials as delivered when buyer views them.
     *
     * @param assignment The assignment to mark as delivered
     */
    @Transactional
    public void markAsDelivered(CredentialAssignment assignment) {
        assignment.setAssignmentStatus(ASSIGNMENT_DELIVERED);
        assignment.setDeliveredAt(LocalDateTime.now());
        credentialAssignmentRepository.save(assignment);

        log.info("Marked credential assignment {} as delivered", assignment.getAssignmentId());
    }

    /**
     * Records when buyer views credentials.
     *
     * @param assignment The assignment viewed
     * @param ipAddress The buyer's IP address
     */
    @Transactional
    public void recordCredentialView(CredentialAssignment assignment, String ipAddress) {
        if (assignment.getFirstViewedAt() == null) {
            assignment.setFirstViewedAt(LocalDateTime.now());
            assignment.setAssignmentStatus(ASSIGNMENT_VIEWED);
        }
        assignment.setViewCount(assignment.getViewCount() + 1);
        assignment.setLastViewIp(ipAddress);
        credentialAssignmentRepository.save(assignment);

        log.info("Recorded view for credential assignment {}, total views: {}", 
                assignment.getAssignmentId(), assignment.getViewCount());
    }

    /**
     * Marks credentials as confirmed after buyer confirmation.
     *
     * @param order The confirmed order
     */
    @Transactional
    public void markCredentialsAsConfirmed(Order order) {
        CredentialStatus soldStatus = credentialStatusRepository.findByStatusName(STATUS_SOLD)
                .orElseThrow(() -> new IllegalStateException("Sold status not found"));

        for (OrderItem item : order.getOrderItems()) {
            List<CredentialAssignment> assignments = credentialAssignmentRepository.findByOrderItem(item);

            for (CredentialAssignment assignment : assignments) {
                PostCredential credential = assignment.getCredential();
                credential.setCredentialStatus(soldStatus);
                postCredentialRepository.save(credential);

                assignment.setAssignmentStatus(ASSIGNMENT_CONFIRMED);
                assignment.setConfirmedAt(LocalDateTime.now());
                credentialAssignmentRepository.save(assignment);
            }

            syncPostStockStatus(item.getPost());
        }

        // Create audit log
        createAuditLog(order.getBuyer(), "CREDENTIALS_CONFIRMED", "Order", order.getOrderId(),
                String.format("Buyer confirmed credentials for order %s", order.getOrderNumber()),
                AuditLog.ROLE_BUYER);

        log.info("Marked credentials as confirmed for order: {}", order.getOrderNumber());
    }

    /**
     * Marks credentials as disputed when buyer opens a dispute.
     *
     * @param order The disputed order
     * @param reason The dispute reason
     */
    @Transactional
    public void markCredentialsAsDisputed(Order order, String reason) {
        for (OrderItem item : order.getOrderItems()) {
            List<CredentialAssignment> assignments = credentialAssignmentRepository.findByOrderItem(item);

            for (CredentialAssignment assignment : assignments) {
                assignment.setAssignmentStatus(ASSIGNMENT_DISPUTED);
                assignment.setStatusReason(reason);
                credentialAssignmentRepository.save(assignment);
            }
        }

        // Create audit log
        createAuditLog(order.getBuyer(), "CREDENTIALS_DISPUTED", "Order", order.getOrderId(),
                String.format("Credentials disputed for order %s. Reason: %s", order.getOrderNumber(), reason),
                AuditLog.ROLE_BUYER);

        log.info("Marked credentials as disputed for order: {}", order.getOrderNumber());
    }

    /**
     * Provides a replacement credential for an invalid one.
     *
     * @param originalAssignmentId The original assignment to replace
     * @param replacementCredential The replacement credential
     * @param reason The reason for replacement
     * @return The replacement assignment
     */
    @Transactional
    public CredentialAssignment provideReplacementCredential(Long originalAssignmentId, 
            PostCredential replacementCredential, String reason) {
        
        CredentialAssignment originalAssignment = credentialAssignmentRepository.findById(originalAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + originalAssignmentId));

        // Mark original as replaced
        originalAssignment.setAssignmentStatus(ASSIGNMENT_REPLACED);
        originalAssignment.setReplacedAt(LocalDateTime.now());
        originalAssignment.setStatusReason(reason);
        credentialAssignmentRepository.save(originalAssignment);

        // Get holding status for replacement
        CredentialStatus holdingStatus = credentialStatusRepository.findByStatusName(STATUS_HOLDING)
                .orElseThrow(() -> new IllegalStateException("Holding status not found"));

        // Mark replacement as holding
        replacementCredential.setCredentialStatus(holdingStatus);
        postCredentialRepository.save(replacementCredential);
        syncPostStockStatus(replacementCredential.getPost());

        // Create new assignment for replacement
        CredentialAssignment newAssignment = CredentialAssignment.builder()
                .orderItem(originalAssignment.getOrderItem())
                .credential(replacementCredential)
                .assignmentStatus(ASSIGNMENT_ASSIGNED)
                .build();
        credentialAssignmentRepository.save(newAssignment);

        // Link the assignments
        newAssignment.setReplacedByAssignment(originalAssignment);
        credentialAssignmentRepository.save(newAssignment);

        // Create audit log
        Order order = originalAssignment.getOrderItem().getOrder();
        createAuditLog(replacementCredential.getPost().getSeller(), "CREDENTIAL_REPLACED", "CredentialAssignment", 
                newAssignment.getAssignmentId(),
                String.format("Credential replaced for order %s. Reason: %s", order.getOrderNumber(), reason),
                AuditLog.ROLE_SELLER);

        // Notify buyer
        notifyBuyer(order, "Replacement Credential Provided",
                String.format("A replacement credential has been provided for your order %s.", order.getOrderNumber()));

        log.info("Replaced credential assignment {} with {} for order: {}", 
                originalAssignmentId, newAssignment.getAssignmentId(), order.getOrderNumber());

        return newAssignment;
    }

    /**
     * Releases reserved credentials back to available pool.
     * Used when payment fails or order is cancelled.
     *
     * @param credentials The credentials to release
     */
    @Transactional
    public void releaseCredentialsReservation(List<PostCredential> credentials) {
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(STATUS_AVAILABLE)
                .orElseThrow(() -> new IllegalStateException("Available status not found"));

        Set<Integer> affectedPostIds = new LinkedHashSet<>();
        for (PostCredential credential : credentials) {
            credential.setCredentialStatus(availableStatus);
            postCredentialRepository.save(credential);
            if (credential.getPost() != null && credential.getPost().getPostId() != null) {
                affectedPostIds.add(credential.getPost().getPostId());
            }
        }

        for (Integer postId : affectedPostIds) {
            syncPostStockStatus(postId);
        }

        log.info("Released {} reserved credentials", credentials.size());
    }

    /**
     * Releases all holding credentials for an order.
     *
     * @param order The cancelled/failed order
     */
    @Transactional
    public void releaseOrderCredentials(Order order) {
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(STATUS_AVAILABLE)
                .orElseThrow(() -> new IllegalStateException("Available status not found"));
        CredentialStatus holdingStatus = credentialStatusRepository.findByStatusName(STATUS_HOLDING)
                .orElseThrow(() -> new IllegalStateException("Holding status not found"));

        int releasedCount = 0;
        Set<Integer> affectedPostIds = new LinkedHashSet<>();
        for (OrderItem item : order.getOrderItems()) {
            PostCredential credential = item.getAssignedCredential();
            if (credential != null
                    && credential.getCredentialStatus() != null
                    && holdingStatus.getStatusName().equalsIgnoreCase(credential.getCredentialStatus().getStatusName())) {
                credential.setCredentialStatus(availableStatus);
                postCredentialRepository.save(credential);
                releasedCount++;
                if (credential.getPost() != null && credential.getPost().getPostId() != null) {
                    affectedPostIds.add(credential.getPost().getPostId());
                }
            }
        }

        for (Integer postId : affectedPostIds) {
            syncPostStockStatus(postId);
        }

        // Create audit log
        createAuditLog(null, "CREDENTIALS_RELEASED", "Order", order.getOrderId(),
                String.format("Released %d reserved credentials for cancelled order %s", releasedCount, order.getOrderNumber()),
                AuditLog.ROLE_SYSTEM);

        log.info("Released {} reserved credentials for order: {}", releasedCount, order.getOrderNumber());
    }

    /**
     * Revokes credential assignments.
     *
     * @param order The order
     * @param reason The revocation reason
     */
    @Transactional
    public void revokeCredentials(Order order, String reason) {
        for (OrderItem item : order.getOrderItems()) {
            List<CredentialAssignment> assignments = credentialAssignmentRepository.findByOrderItem(item);

            for (CredentialAssignment assignment : assignments) {
                assignment.setAssignmentStatus(ASSIGNMENT_REVOKED);
                assignment.setRevokedAt(LocalDateTime.now());
                assignment.setStatusReason(reason);
                credentialAssignmentRepository.save(assignment);
            }
        }

        log.info("Revoked credentials for order: {}", order.getOrderNumber());
    }

    /**
     * Gets all credentials assigned for an order.
     *
     * @param order The order
     * @return List of credential assignments
     */
    public List<CredentialAssignment> getAssignmentsForOrder(Order order) {
        List<CredentialAssignment> allAssignments = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            allAssignments.addAll(credentialAssignmentRepository.findByOrderItem(item));
        }
        return allAssignments;
    }

    /**
     * Gets credentials available for a post.
     *
     * @param post The post
     * @return Count of available credentials
     */
    public long getAvailableCredentialCount(Post post) {
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(STATUS_AVAILABLE)
                .orElse(null);
        if (availableStatus == null) return 0;
        return postCredentialRepository.countByPost_PostIdAndCredentialStatus(post.getPostId(), availableStatus);
    }

    /**
     * Checks if a post has enough available credentials.
     *
     * @param post The post
     * @param quantity Required quantity
     * @return true if enough credentials available
     */
    public boolean hasEnoughCredentials(Post post, int quantity) {
        long available = getAvailableCredentialCount(post);
        return available >= quantity;
    }

    private void syncPostStockStatus(Post post) {
        if (post == null || post.getPostId() == null) {
            return;
        }
        syncPostStockStatus(post.getPostId());
    }

    private void syncPostStockStatus(Integer postId) {
        if (postId == null) {
            return;
        }
        postService.updateStockStatus(postId);
    }

    /**
     * Gets credential assignment history for a buyer.
     *
     * @param buyerId The buyer user ID
     * @return List of assignments
     */
    public List<CredentialAssignment> getBuyerCredentialHistory(Long buyerId) {
        return credentialAssignmentRepository.findByBuyerIdOrderByAssignedAtDesc(buyerId);
    }

    /**
     * Gets credential assignment history for a seller.
     *
     * @param sellerId The seller user ID
     * @return List of assignments
     */
    public List<CredentialAssignment> getSellerCredentialHistory(Long sellerId) {
        return credentialAssignmentRepository.findBySellerIdOrderByAssignedAtDesc(sellerId);
    }

    /**
     * Gets a credential assignment by ID.
     *
     * @param assignmentId The assignment ID
     * @return The assignment if found
     */
    public Optional<CredentialAssignment> getAssignmentById(Long assignmentId) {
        return credentialAssignmentRepository.findById(assignmentId);
    }

    /**
     * Creates an audit log entry.
     */
    private void createAuditLog(User user, String action, String entityType,
            Long entityId, String description, String performerRole) {
        
        AuditLog auditLog = AuditLog.builder()
                .eventType(AuditLog.EVENT_CREDENTIAL)
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
     * Notifies the buyer about credential updates.
     */
    private void notifyBuyer(Order order, String title, String message) {
        notificationService.createNotification(
                order.getBuyer(),
                Notification.TYPE_CREDENTIAL,
                NotificationPreference.CATEGORY_CREDENTIAL,
                title,
                message,
                Notification.PRIORITY_HIGH,
                "ORDER",
                order.getOrderId(),
                "/buyer/purchases?orderId=" + order.getOrderId()
        );
    }
}
