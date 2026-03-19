package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing escrow operations in the transaction workflow.
 * 
 * Escrow Lifecycle:
 * 1. Created when payment is confirmed (status: HOLDING)
 * 2. Frozen when dispute is opened (status: FROZEN)
 * 3. Released to seller after buyer confirmation or timeout (status: RELEASED)
 * 4. Refunded to buyer if dispute resolved in buyer's favor (status: REFUNDED)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final EscrowStatusRepository escrowStatusRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CredentialService credentialService;

    @Value("${escrow.verification-timeout-hours:24}")
    private int verificationTimeoutHours;

    @Value("${escrow.platform-fee-percent:5}")
    private BigDecimal platformFeePercent;

    // Escrow status constants
    public static final String STATUS_NOT_CREATED = "NOT_CREATED";
    public static final String STATUS_HOLDING = "HOLDING";
    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";

    /**
     * Creates an escrow record for a paid order.
     * Called automatically after successful payment confirmation.
     *
     * @param order The paid order
     * @return The created escrow record
     */
    @Transactional
    public Escrow createEscrow(Order order) {
        // Check if escrow already exists
        Optional<Escrow> existingEscrow = escrowRepository.findByOrder(order);
        if (existingEscrow.isPresent()) {
            log.info("Escrow already exists for order: {}", order.getOrderNumber());
            return existingEscrow.get();
        }

        // Calculate amounts
        BigDecimal grossAmount = order.getTotalAmount();
        BigDecimal platformFee = calculatePlatformFee(grossAmount);
        BigDecimal sellerAmount = grossAmount.subtract(platformFee);

        // Get holding status
        EscrowStatus holdingStatus = escrowStatusRepository.findByStatusName(STATUS_HOLDING)
                .orElseThrow(() -> new IllegalStateException("HOLDING status not found"));

        // Calculate release time (buyer has X hours to verify credentials)
        LocalDateTime autoReleaseDeadline = LocalDateTime.now().plusHours(verificationTimeoutHours);

        // Create escrow
        Escrow escrow = Escrow.builder()
                .order(order)
                .escrowStatus(holdingStatus)
                .amount(grossAmount)
                .platformFee(platformFee)
                .sellerAmount(sellerAmount)
                .autoReleaseDeadline(autoReleaseDeadline)
                .build();

        escrowRepository.save(escrow);

        // Create escrow transaction for HOLD
        createEscrowTransaction(escrow, EscrowTransaction.TYPE_CREATED, grossAmount, grossAmount,
                "Initial escrow hold after successful payment", null, null, null);

        // Create audit log
        createAuditLog(null, "ESCROW_CREATED", "Escrow", escrow.getEscrowId(),
                String.format("Escrow created for order %s, amount: %s", order.getOrderNumber(), grossAmount),
                AuditLog.ROLE_SYSTEM);

        log.info("Created escrow for order: {}, amount: {}, release scheduled: {}",
                order.getOrderNumber(), grossAmount, autoReleaseDeadline);

        return escrow;
    }

    /**
     * Releases escrow funds to the seller.
     * Called when buyer confirms receipt or after timeout.
     *
     * @param orderId The order ID
     * @param reason  The reason for release
     * @param releasedBy User ID who triggered the release (null for automatic)
     * @return The updated escrow
     */
    @Transactional
    public Escrow releaseEscrow(Long orderId, String reason, Integer releasedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        Escrow escrow = escrowRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalArgumentException("Escrow not found for order: " + orderId));

        // Check if escrow can be released
        String currentStatus = escrow.getEscrowStatus().getStatusName();
        if (!currentStatus.equals(STATUS_HOLDING) && !currentStatus.equals(STATUS_FROZEN)) {
            throw new IllegalStateException("Escrow cannot be released from status: " + currentStatus);
        }

        // Get released status
        EscrowStatus releasedStatus = escrowStatusRepository.findByStatusName(STATUS_RELEASED)
                .orElseThrow(() -> new IllegalStateException("RELEASED status not found"));

        User releasingUser = releasedBy != null ? userRepository.findById(releasedBy).orElse(null) : null;
        User adminUser = isAdminUser(releasingUser) ? releasingUser : null;

        // Update escrow
        escrow.setEscrowStatus(releasedStatus);
        escrow.setReleasedAt(LocalDateTime.now());
        escrow.setProcessedByAdmin(adminUser);
        escrow.setAutoReleaseDeadline(null);
        escrow.setStatusReason(reason);
        escrowRepository.save(escrow);

        // Create escrow transaction
        createEscrowTransaction(escrow, EscrowTransaction.TYPE_RELEASED, escrow.getSellerAmount(), BigDecimal.ZERO,
                reason, "RELEASE-" + System.currentTimeMillis(), releasingUser, null);

        // Update order status to completed
        OrderStatus completedStatus = orderStatusRepository.findByStatusName(OrderStatus.COMPLETED)
                .orElseThrow(() -> new IllegalStateException("COMPLETED status not found"));
        order.setOrderStatus(completedStatus);
        order.setConfirmedAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        order.setConfirmationDeadline(null);
        orderRepository.save(order);

        markOrderItemsCompleted(order);
        credentialService.markCredentialsAsConfirmed(order);
        creditSellerWallets(order);

        // Create audit log
        String performerRole = adminUser != null
                ? AuditLog.ROLE_ADMIN
                : (releasingUser != null ? AuditLog.ROLE_BUYER : AuditLog.ROLE_SYSTEM);
        createAuditLog(releasingUser, "ESCROW_RELEASED", "Escrow", escrow.getEscrowId(),
                String.format("Escrow released for order %s. Reason: %s", order.getOrderNumber(), reason),
                performerRole);

        // Notify seller
        notifySeller(order, "Escrow Released", 
                String.format("Your escrow for order %s has been released. Amount: %s", 
                        order.getOrderNumber(), escrow.getSellerAmount()));
        notifyBuyer(order, "Order Completed",
                String.format("Order %s has been completed. The seller has been paid and your transaction is now closed.",
                        order.getOrderNumber()));

        log.info("Released escrow for order: {}, amount: {}, reason: {}", orderId, escrow.getSellerAmount(), reason);

        return escrow;
    }

    /**
     * Freezes escrow when a dispute is opened.
     *
     * @param orderId The order ID
     * @param reason  The reason for freezing
     * @return The updated escrow
     */
    @Transactional
    public Escrow freezeEscrow(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        Escrow escrow = escrowRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalArgumentException("Escrow not found for order: " + orderId));

        // Can only freeze from HOLDING status
        if (!escrow.getEscrowStatus().getStatusName().equals(STATUS_HOLDING)) {
            throw new IllegalStateException("Escrow can only be frozen from HOLDING status");
        }

        // Get frozen status
        EscrowStatus frozenStatus = escrowStatusRepository.findByStatusName(STATUS_FROZEN)
                .orElseThrow(() -> new IllegalStateException("FROZEN status not found"));

        BigDecimal currentBalance = escrow.getAmount().subtract(escrow.getPlatformFee());

        // Update escrow
        escrow.setEscrowStatus(frozenStatus);
        escrow.setFrozenAt(LocalDateTime.now());
        escrow.setAutoReleaseDeadline(null); // Clear release schedule while frozen
        escrow.setStatusReason(reason);
        escrowRepository.save(escrow);

        // Create escrow transaction
        createEscrowTransaction(escrow, EscrowTransaction.TYPE_FROZEN, BigDecimal.ZERO, currentBalance,
                reason, null, null, null);

        // Create audit log
        createAuditLog(null, "ESCROW_FROZEN", "Escrow", escrow.getEscrowId(),
                String.format("Escrow frozen for order %s. Reason: %s", order.getOrderNumber(), reason),
                AuditLog.ROLE_SYSTEM);

        log.info("Frozen escrow for order: {}, reason: {}", orderId, reason);

        return escrow;
    }

    /**
     * Unfreezes escrow when a dispute is resolved in seller's favor.
     *
     * @param orderId The order ID
     * @param reason  The reason for unfreezing
     * @return The updated escrow
     */
    @Transactional
    public Escrow unfreezeEscrow(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        Escrow escrow = escrowRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalArgumentException("Escrow not found for order: " + orderId));

        // Can only unfreeze from FROZEN status
        if (!escrow.getEscrowStatus().getStatusName().equals(STATUS_FROZEN)) {
            throw new IllegalStateException("Escrow can only be unfrozen from FROZEN status");
        }

        // Get holding status
        EscrowStatus holdingStatus = escrowStatusRepository.findByStatusName(STATUS_HOLDING)
                .orElseThrow(() -> new IllegalStateException("HOLDING status not found"));

        BigDecimal currentBalance = escrow.getAmount().subtract(escrow.getPlatformFee());

        // Update escrow - set new release time
        escrow.setEscrowStatus(holdingStatus);
        escrow.setAutoReleaseDeadline(LocalDateTime.now().plusHours(verificationTimeoutHours));
        escrow.setStatusReason(reason);
        escrowRepository.save(escrow);

        // Create escrow transaction
        createEscrowTransaction(escrow, EscrowTransaction.TYPE_UNFROZEN, BigDecimal.ZERO, currentBalance,
                reason, null, null, null);

        // Create audit log
        createAuditLog(null, "ESCROW_UNFROZEN", "Escrow", escrow.getEscrowId(),
                String.format("Escrow unfrozen for order %s. Reason: %s", order.getOrderNumber(), reason),
                AuditLog.ROLE_SYSTEM);

        log.info("Unfrozen escrow for order: {}, reason: {}", orderId, reason);

        return escrow;
    }

    /**
     * Refunds escrow to buyer.
     *
     * @param orderId The order ID
     * @param amount  The refund amount (null for full refund)
     * @param reason  The reason for refund
     * @param processedBy Admin ID who processed the refund
     * @return The updated escrow
     */
    @Transactional
    public Escrow refundEscrow(Long orderId, BigDecimal amount, String reason, Integer processedBy) {
        log.info("[DEBUG] refundEscrow - Starting for orderId: {}, amount: {}, processedBy: {}",
                orderId, amount, processedBy);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("[DEBUG] Order not found: {}", orderId);
                    return new IllegalArgumentException("Order not found: " + orderId);
                });
        log.info("[DEBUG] Found order: {}", order.getOrderNumber());
        
        Escrow escrow = escrowRepository.findByOrder(order)
                .orElseThrow(() -> {
                    log.error("[DEBUG] Escrow not found for order: {}", orderId);
                    return new IllegalArgumentException("Escrow not found for order: " + orderId);
                });
        log.info("[DEBUG] Found escrow: {}, status: {}", escrow.getEscrowId(), escrow.getEscrowStatus().getStatusName());

        // Can refund from HOLDING or FROZEN status
        String currentStatus = escrow.getEscrowStatus().getStatusName();
        log.info("[DEBUG] Current escrow status: {}", currentStatus);
        
        if (!currentStatus.equals(STATUS_HOLDING) && !currentStatus.equals(STATUS_FROZEN)) {
            log.error("[DEBUG] Cannot refund from status: {}", currentStatus);
            throw new IllegalStateException("Escrow cannot be refunded from status: " + currentStatus);
        }

        // Determine refund amount
        BigDecimal refundAmount = amount != null ? amount : escrow.getAmount();
        boolean isPartialRefund = refundAmount.compareTo(escrow.getAmount()) < 0;

        // Get appropriate status
        String targetStatus = isPartialRefund ? STATUS_PARTIALLY_REFUNDED : STATUS_REFUNDED;
        EscrowStatus refundStatus = escrowStatusRepository.findByStatusName(targetStatus)
                .orElseThrow(() -> new IllegalStateException(targetStatus + " status not found"));

        User adminUser = processedBy != null ? userRepository.findById(processedBy).orElse(null) : null;
        BigDecimal newBalance = isPartialRefund ? escrow.getAmount().subtract(escrow.getPlatformFee()).subtract(refundAmount) : BigDecimal.ZERO;

        // Update escrow
        escrow.setEscrowStatus(refundStatus);
        escrow.setRefundedAt(LocalDateTime.now());
        escrow.setProcessedByAdmin(adminUser);
        escrow.setStatusReason(reason);
        escrowRepository.save(escrow);

        // Create escrow transaction
        String transactionType = isPartialRefund ? EscrowTransaction.TYPE_PARTIAL_REFUND : EscrowTransaction.TYPE_REFUNDED;
        createEscrowTransaction(escrow, transactionType, refundAmount, newBalance,
                reason, "REFUND-" + System.currentTimeMillis(), adminUser, null);

        // Update order status
        if (!isPartialRefund) {
            OrderStatus refundedStatus = orderStatusRepository.findByStatusName(OrderStatus.REFUNDED)
                    .orElseThrow(() -> new IllegalStateException("REFUNDED status not found"));
            order.setOrderStatus(refundedStatus);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancellationReason(reason);
            orderRepository.save(order);
        }

        // Credit buyer's wallet
        User buyer = order.getBuyer();
        Wallet buyerWallet = walletRepository.findByUser(buyer)
                .orElseGet(() -> Wallet.builder()
                        .user(buyer)
                        .balance(BigDecimal.ZERO)
                        .frozenBalance(BigDecimal.ZERO)
                        .build());
        BigDecimal currentBalance = buyerWallet.getBalance() != null ? buyerWallet.getBalance() : BigDecimal.ZERO;
        buyerWallet.setBalance(currentBalance.add(refundAmount));
        walletRepository.save(buyerWallet);
        log.info("[DEBUG] Credited buyer wallet - userId: {}, amount: {}, newBalance: {}",
                buyer.getUserId(), refundAmount, buyerWallet.getBalance());

        // Create audit log
        createAuditLog(adminUser, "ESCROW_REFUNDED", "Escrow", escrow.getEscrowId(),
                String.format("Escrow refunded for order %s. Amount: %s. Reason: %s",
                        order.getOrderNumber(), refundAmount, reason),
                AuditLog.ROLE_ADMIN);

        // Notify buyer
        notifyBuyer(order, "Refund Processed",
                String.format("Your refund for order %s has been processed. Amount: %s",
                        order.getOrderNumber(), refundAmount));

        log.info("Refunded escrow for order: {}, amount: {}, reason: {}", orderId, refundAmount, reason);

        return escrow;
    }

    /**
     * Scheduled task to auto-release escrows after verification timeout.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void autoReleaseEscrows() {
        log.info("Running auto-release escrow check...");
        
        // Find escrows in HOLDING status past their release time
        List<Escrow> escrowsToRelease = escrowRepository.findExpiredEscrows(STATUS_HOLDING, LocalDateTime.now());

        for (Escrow escrow : escrowsToRelease) {
            try {
                releaseEscrow(escrow.getOrder().getOrderId(), "Auto-release after buyer verification timeout", null);
                log.info("Auto-released escrow for order: {}", escrow.getOrder().getOrderNumber());
            } catch (Exception e) {
                log.error("Failed to auto-release escrow for order: {}", escrow.getOrder().getOrderNumber(), e);
            }
        }

        log.info("Auto-release check completed. Released {} escrows", escrowsToRelease.size());
    }

    /**
     * Gets escrow details for an order.
     *
     * @param orderId The order ID
     * @return The escrow if found
     */
    public Optional<Escrow> getEscrowByOrderId(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(escrowRepository::findByOrder);
    }

    /**
     * Gets all escrow transactions for an escrow.
     *
     * @param escrow The escrow
     * @return List of transactions
     */
    public List<EscrowTransaction> getEscrowTransactions(Escrow escrow) {
        return escrowTransactionRepository.findByEscrowOrderByCreatedAtDesc(escrow);
    }

    /**
     * Calculates the platform fee based on the gross amount.
     *
     * @param grossAmount The gross amount
     * @return The platform fee
     */
    private BigDecimal calculatePlatformFee(BigDecimal grossAmount) {
        BigDecimal feePercent = platformFeePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return grossAmount.multiply(feePercent).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Creates an escrow transaction record.
     */
    private void createEscrowTransaction(Escrow escrow, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter, String description, String reference, User initiatedBy, String initiatorIp) {
        
        EscrowTransaction transaction = EscrowTransaction.builder()
                .escrow(escrow)
                .transactionType(transactionType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .initiatedBy(initiatedBy)
                .initiatorIp(initiatorIp)
                .build();

        if (reference != null) {
            transaction.setReferenceType("REFERENCE");
            transaction.setReferenceId(System.currentTimeMillis());
        }

        escrowTransactionRepository.save(transaction);
    }

    private void markOrderItemsCompleted(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            item.setItemStatus(OrderItem.STATUS_COMPLETED);
            item.setConfirmedAt(LocalDateTime.now());
            item.setCompletedAt(LocalDateTime.now());
        }
    }

    private void creditSellerWallets(Order order) {
        java.util.Map<User, BigDecimal> sellerEarnings = new java.util.HashMap<>();

        for (OrderItem item : order.getOrderItems()) {
            User seller = item.getSeller() != null ? item.getSeller() : (item.getPost() != null ? item.getPost().getSeller() : null);
            if (seller == null) {
                continue;
            }

            BigDecimal earnings = item.getSellerEarnings() != null
                    ? item.getSellerEarnings()
                    : item.getUnitPrice().subtract(item.getPlatformFee() != null ? item.getPlatformFee() : BigDecimal.ZERO);
            sellerEarnings.merge(seller, earnings, BigDecimal::add);
        }

        for (java.util.Map.Entry<User, BigDecimal> entry : sellerEarnings.entrySet()) {
            Wallet wallet = walletRepository.findByUser(entry.getKey())
                    .orElseGet(() -> Wallet.builder().user(entry.getKey()).balance(BigDecimal.ZERO).frozenBalance(BigDecimal.ZERO).build());
            wallet.setBalance((wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO).add(entry.getValue()));
            walletRepository.save(wallet);
        }
    }

    private boolean isAdminUser(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && "Admin".equalsIgnoreCase(user.getRole().getRoleName());
    }

    /**
     * Creates an audit log entry.
     */
    private void createAuditLog(User user, String action, String entityType,
            Long entityId, String description, String performerRole) {
        
        AuditLog auditLog = AuditLog.builder()
                .eventType(AuditLog.EVENT_ESCROW)
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
     * Notifies the seller about escrow changes.
     */
    private void notifySeller(Order order, String title, String message) {
        // Get seller from first order item
        List<OrderItem> items = order.getOrderItems();
        if (items.isEmpty()) return;

        Post post = items.get(0).getPost();
        User seller = post.getSeller();

        Notification notification = Notification.builder()
                .user(seller)
                .notificationType(Notification.TYPE_ESCROW)
                .title(title)
                .message(message)
                .relatedEntityType("ORDER")
                .relatedEntityId(order.getOrderId())
                .priority(Notification.PRIORITY_HIGH)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Notifies the buyer about escrow changes.
     */
    private void notifyBuyer(Order order, String title, String message) {
        Notification notification = Notification.builder()
                .user(order.getBuyer())
                .notificationType(Notification.TYPE_ESCROW)
                .title(title)
                .message(message)
                .relatedEntityType("ORDER")
                .relatedEntityId(order.getOrderId())
                .priority(Notification.PRIORITY_HIGH)
                .build();

        notificationRepository.save(notification);
    }
}
