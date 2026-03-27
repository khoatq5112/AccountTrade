package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.CredentialAssignment;
import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.PostCredential;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CredentialAssignmentRepository;
import com.group3.accounttrade.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuyerOrderService {

    private final OrderRepository orderRepository;
    private final CredentialAssignmentRepository credentialAssignmentRepository;
    private final CredentialService credentialService;
    private final EscrowService escrowService;

    @Transactional(readOnly = true)
    public List<Order> getOrders(User buyer) {
        return orderRepository.findByBuyer(buyer).stream()
                .filter(order -> !hasStatus(order, OrderStatus.PAYMENT_FAILED))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public BuyerDashboardSummary getDashboardSummary(User buyer) {
        List<Order> orders = getOrders(buyer);

        List<Order> escrowOrders = orders.stream()
                .filter(this::isInEscrow)
                .toList();

        List<Order> completedOrders = orders.stream()
                .filter(order -> hasStatus(order, OrderStatus.COMPLETED))
                .toList();

        List<Order> pendingOrders = orders.stream()
                .filter(this::isActiveOrder)
                .limit(5)
                .toList();

        return BuyerDashboardSummary.builder()
                .orders(orders)
                .escrowAmount(sumAmounts(escrowOrders))
                .escrowCount(escrowOrders.size())
                .totalSpent(sumAmounts(completedOrders))
                .completedCount(completedOrders.size())
                .pendingOrders(pendingOrders)
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<PostCredential> getPurchasedCredential(User buyer, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getBuyer().getUserId().equals(buyer.getUserId()))
                .flatMap(order -> order.getOrderItems().stream()
                        .map(OrderItem::getAssignedCredential)
                        .filter(credential -> credential != null)
                        .findFirst());
    }

    @Transactional
    public List<BuyerCredentialView> revealCredentials(User buyer, Long orderId, String ipAddress) {
        Order order = orderRepository.findById(orderId)
                .filter(existing -> existing.getBuyer().getUserId().equals(buyer.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getCredentialAssignedAt() == null) {
            throw new IllegalStateException("Credentials are not ready for this order yet");
        }

        return order.getOrderItems().stream()
                .flatMap(item -> credentialAssignmentRepository.findByOrderItemOrderByAssignedAtDesc(item).stream())
                .map(assignment -> {
                    credentialService.recordCredentialView(assignment, ipAddress);
                    PostCredential credential = assignment.getCredential();
                    return BuyerCredentialView.builder()
                            .assignmentId(assignment.getAssignmentId())
                            .orderItemId(assignment.getOrderItem() != null ? assignment.getOrderItem().getOrderItemId() : null)
                            .postTitle(assignment.getOrderItem() != null ? assignment.getOrderItem().getPostTitleSnapshot() : null)
                            .assignmentStatus(assignment.getAssignmentStatus())
                            .assignedAt(assignment.getAssignedAt())
                            .deliveredAt(assignment.getDeliveredAt())
                            .firstViewedAt(assignment.getFirstViewedAt())
                            .viewCount(assignment.getViewCount())
                            .accountUsername(credential.getAccountUsername())
                            .accountPassword(credential.getAccountPassword())
                            .securityNotes(credential.getSecurityNotes())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void confirmReceipt(User buyer, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(existing -> existing.getBuyer().getUserId().equals(buyer.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!hasStatus(order, OrderStatus.AWAITING_BUYER_CONFIRMATION)) {
            throw new IllegalStateException("Order is not waiting for buyer confirmation");
        }

        if (order.getCredentialAssignedAt() == null) {
            throw new IllegalStateException("Credentials have not been delivered for this order yet");
        }

        escrowService.releaseEscrow(orderId, "Buyer confirmed receipt", buyer.getUserId());
    }

    private boolean isInEscrow(Order order) {
        return hasStatus(order, OrderStatus.PAID)
                || hasStatus(order, OrderStatus.PROCESSING)
                || hasStatus(order, OrderStatus.CREDENTIAL_ASSIGNED)
                || hasStatus(order, OrderStatus.AWAITING_BUYER_CONFIRMATION)
                || hasStatus(order, OrderStatus.DISPUTED);
    }

    private boolean isActiveOrder(Order order) {
        return isInEscrow(order) || hasStatus(order, OrderStatus.AWAITING_PAYMENT);
    }

    private boolean hasStatus(Order order, String statusName) {
        return order.getOrderStatus() != null
                && statusName.equalsIgnoreCase(order.getOrderStatus().getStatusName());
    }

    private BigDecimal sumAmounts(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Builder
    public record BuyerDashboardSummary(
            List<Order> orders,
            BigDecimal escrowAmount,
            long escrowCount,
            BigDecimal totalSpent,
            long completedCount,
            List<Order> pendingOrders
    ) {
    }

    /**
     * View model for displaying credential information to buyers.
     * Uses Lombok @Getter to generate standard JavaBean getters for Thymeleaf compatibility.
     */
    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BuyerCredentialView {
            private Long assignmentId;
            private Long orderItemId;
            private String postTitle;
            private String assignmentStatus;
            private java.time.LocalDateTime assignedAt;
            private java.time.LocalDateTime deliveredAt;
            private java.time.LocalDateTime firstViewedAt;
            private Integer viewCount;
            private String accountUsername;
            private String accountPassword;
            private String securityNotes;
    }
}
