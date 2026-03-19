package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.EscrowRepository;
import com.group3.accounttrade.repository.OrderItemRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for gathering seller dashboard statistics and data.
 */
@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EscrowRepository escrowRepository;
    private final PostRepository postRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * Gets dashboard statistics for a seller.
     *
     * @param username the seller's username
     * @return SellerDashboardStats containing all dashboard metrics
     */
    @Transactional(readOnly = true)
    public SellerDashboardStats getDashboardStats(String username) {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get wallet info
        Wallet wallet = walletRepository.findByUser(seller).orElse(null);

        // Get post counts
        long totalPosts = postRepository.countBySeller_Username(username);
        long activePosts = postRepository.countBySeller_UsernameAndStockStatus(username, StockStatus.IN_STOCK);

        List<Order> sellerOrders = orderRepository.findOrdersContainingSellerPosts(seller);
        long completedOrders = sellerOrders.stream()
                .filter(order -> hasStatus(order, OrderStatus.COMPLETED))
                .count();

        long escrowOrders = escrowRepository.countBySellerAndStatus(seller, EscrowStatus.HOLDING)
                + escrowRepository.countBySellerAndStatus(seller, EscrowStatus.FROZEN);

        BigDecimal totalRevenue = sellerOrders.stream()
                .filter(order -> hasStatus(order, OrderStatus.COMPLETED))
                .flatMap(order -> order.getOrderItems().stream())
                .filter(item -> item.getSeller() != null && seller.getUserId().equals(item.getSeller().getUserId()))
                .map(item -> item.getSellerEarnings() != null ? item.getSellerEarnings() : item.getUnitPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal escrowAmount = escrowRepository.sumAmountBySellerAndStatus(seller, EscrowStatus.HOLDING);
        BigDecimal frozenEscrowAmount = escrowRepository.sumAmountBySellerAndStatus(seller, EscrowStatus.FROZEN);
        escrowAmount = (escrowAmount != null ? escrowAmount : BigDecimal.ZERO)
                .add(frozenEscrowAmount != null ? frozenEscrowAmount : BigDecimal.ZERO);

        return new SellerDashboardStats(
                wallet != null ? wallet.getBalance() : BigDecimal.ZERO,
                wallet != null ? wallet.getFrozenBalance() : BigDecimal.ZERO,
                totalPosts,
                activePosts,
                escrowOrders,
                completedOrders,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                escrowAmount != null ? escrowAmount : BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public Page<SellerOrderRow> getSellerOrders(String username, String keyword, String status, Pageable pageable) {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedKeyword = keyword != null ? keyword.trim() : null;
        String normalizedStatus = status != null && !status.isBlank() ? status.trim() : null;

        return orderItemRepository.findSellerOrderItems(seller, normalizedKeyword, normalizedStatus, pageable)
                .map(this::toSellerOrderRow);
    }

    private boolean hasStatus(Order order, String statusName) {
        return order.getOrderStatus() != null
                && statusName.equalsIgnoreCase(order.getOrderStatus().getStatusName());
    }

    private SellerOrderRow toSellerOrderRow(OrderItem orderItem) {
        Order order = orderItem.getOrder();

        return new SellerOrderRow(
                order.getOrderId(),
                order.getOrderNumber(),
                orderItem.getPost() != null ? orderItem.getPost().getPostId() : null,
                orderItem.getPostTitleSnapshot(),
                order.getBuyer() != null ? order.getBuyer().getUsername() : "Unknown buyer",
                orderItem.getUnitPrice(),
                orderItem.getSellerEarnings(),
                order.getOrderStatus() != null ? order.getOrderStatus().getStatusName() : OrderStatus.PENDING,
                orderItem.getItemStatus(),
                order.getCreatedAt(),
                order.getConfirmationDeadline()
        );
    }

    /**
     * DTO for seller dashboard statistics.
     */
    public record SellerDashboardStats(
            BigDecimal availableBalance,
            BigDecimal frozenBalance,
            long totalPosts,
            long activePosts,
            long escrowOrders,
            long completedOrders,
            BigDecimal totalRevenue,
            BigDecimal escrowAmount
    ) {
        public String getFormattedAvailableBalance() {
            return formatCurrency(availableBalance);
        }

        public String getFormattedFrozenBalance() {
            return formatCurrency(frozenBalance);
        }

        public String getFormattedTotalRevenue() {
            return formatCurrency(totalRevenue);
        }

        public String getFormattedEscrowAmount() {
            return formatCurrency(escrowAmount);
        }

        private String formatCurrency(BigDecimal amount) {
            if (amount == null) return "0₫";
            return String.format("%,.0f₫", amount);
        }
    }

    public record SellerOrderRow(
            Long orderId,
            String orderNumber,
            Integer postId,
            String productTitle,
            String buyerUsername,
            BigDecimal saleAmount,
            BigDecimal sellerEarnings,
            String orderStatus,
            String itemStatus,
            LocalDateTime createdAt,
            LocalDateTime confirmationDeadline
    ) {
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        public String getFormattedSaleAmount() {
            BigDecimal amount = saleAmount != null ? saleAmount : BigDecimal.ZERO;
            return String.format("%,.0f ₫", amount);
        }

        public String getFormattedSellerEarnings() {
            BigDecimal amount = sellerEarnings != null ? sellerEarnings : saleAmount;
            amount = amount != null ? amount : BigDecimal.ZERO;
            return String.format("%,.0f ₫", amount);
        }

        public String getFormattedCreatedAt() {
            return createdAt != null ? createdAt.format(DATE_TIME_FORMATTER) : "-";
        }

        public String getFormattedConfirmationDeadline() {
            return confirmationDeadline != null ? confirmationDeadline.format(DATE_TIME_FORMATTER) : null;
        }

        public boolean isDisputed() {
            return OrderStatus.DISPUTED.equalsIgnoreCase(orderStatus);
        }

        public boolean isAwaitingBuyerConfirmation() {
            return OrderStatus.AWAITING_BUYER_CONFIRMATION.equalsIgnoreCase(orderStatus);
        }
    }
}
