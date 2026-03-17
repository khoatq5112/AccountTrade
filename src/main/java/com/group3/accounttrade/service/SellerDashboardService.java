package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for gathering seller dashboard statistics and data.
 */
@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private final TransactionRepository transactionRepository;
    private final PostRepository postRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionStatusRepository transactionStatusRepository;

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

        // Get transaction stats
        TransactionStatus pendingStatus = transactionStatusRepository.findByStatusName("PENDING").orElse(null);
        TransactionStatus escrowStatus = transactionStatusRepository.findByStatusName("ESCROW").orElse(null);
        TransactionStatus completedStatus = transactionStatusRepository.findByStatusName("COMPLETED").orElse(null);

        long pendingOrders = pendingStatus != null ?
                transactionRepository.countBySellerAndStatus(seller, pendingStatus) : 0;
        long escrowOrders = escrowStatus != null ?
                transactionRepository.countBySellerAndStatus(seller, escrowStatus) : 0;
        long completedOrders = completedStatus != null ?
                transactionRepository.countBySellerAndStatus(seller, completedStatus) : 0;

        BigDecimal totalRevenue = transactionRepository.sumRevenueBySellerAndStatusName(seller, "COMPLETED");
        BigDecimal escrowAmount = transactionRepository.sumRevenueBySellerAndStatusName(seller, "ESCROW");

        return new SellerDashboardStats(
                wallet != null ? wallet.getBalance() : BigDecimal.ZERO,
                wallet != null ? wallet.getFrozenBalance() : BigDecimal.ZERO,
                totalPosts,
                activePosts,
                pendingOrders,
                escrowOrders,
                completedOrders,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                escrowAmount != null ? escrowAmount : BigDecimal.ZERO
        );
    }

    /**
     * Gets pending orders for a seller (orders requiring action).
     *
     * @param username the seller's username
     * @param limit    maximum number of orders to return
     * @return list of pending transactions
     */
    @Transactional(readOnly = true)
    public List<Transaction> getPendingOrders(String username, int limit) {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        TransactionStatus pendingStatus = transactionStatusRepository.findByStatusName("PENDING")
                .orElse(null);
        TransactionStatus escrowStatus = transactionStatusRepository.findByStatusName("ESCROW")
                .orElse(null);

        // Build list of statuses, filtering out nulls
        java.util.ArrayList<TransactionStatus> statuses = new java.util.ArrayList<>();
        if (pendingStatus != null) {
            statuses.add(pendingStatus);
        }
        if (escrowStatus != null) {
            statuses.add(escrowStatus);
        }

        if (statuses.isEmpty()) {
            return List.of();
        }

        // Get orders that are pending or in escrow (requiring seller action)
        List<Transaction> orders = transactionRepository.findBySellerAndStatusInOrderByCreatedAtDesc(
                seller, statuses, PageRequest.of(0, limit));

        return orders;
    }

    /**
     * DTO for seller dashboard statistics.
     */
    public record SellerDashboardStats(
            BigDecimal availableBalance,
            BigDecimal frozenBalance,
            long totalPosts,
            long activePosts,
            long pendingOrders,
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

        public long getTotalOrdersRequiringAction() {
            return pendingOrders + escrowOrders;
        }

        private String formatCurrency(BigDecimal amount) {
            if (amount == null) return "0₫";
            return String.format("%,.0f₫", amount);
        }
    }
}
