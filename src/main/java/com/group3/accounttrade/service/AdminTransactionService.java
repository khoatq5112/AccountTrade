package com.group3.accounttrade.service;

import com.group3.accounttrade.dto.TransactionDTO;
import com.group3.accounttrade.dto.TransactionStatsDTO;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTransactionService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Page<TransactionDTO> getAllTransactions(String keyword, String status,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {

        // Fetch paginated orders from DB with eager fetching of buyer, orderStatus, orderItems, sellers
        Page<Order> orderPage = orderRepository.findAllForAdmin(pageable);

        List<TransactionDTO> dtos = orderPage.getContent().stream()
                .filter(order -> filterByStatus(order, status))
                .filter(order -> filterByDateRange(order, from, to))
                .filter(order -> filterByKeyword(order, keyword))
                .map(this::mapOrder)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, orderPage.getTotalElements());
    }

    private boolean filterByStatus(Order order, String status) {
        if (status == null || status.isEmpty()) return true;
        String orderStatus = order.getOrderStatus() != null
                ? order.getOrderStatus().getStatusName() : null;
        return status.equalsIgnoreCase(orderStatus);
    }

    private boolean filterByDateRange(Order order, LocalDateTime from, LocalDateTime to) {
        if (from != null && order.getCreatedAt() != null && order.getCreatedAt().isBefore(from)) {
            return false;
        }
        if (to != null && order.getCreatedAt() != null && order.getCreatedAt().isAfter(to)) {
            return false;
        }
        return true;
    }

    private boolean filterByKeyword(Order order, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true;
        String kw = keyword.toLowerCase();
        // Match order number
        if (order.getOrderNumber() != null && order.getOrderNumber().toLowerCase().contains(kw)) {
            return true;
        }
        // Match buyer username
        if (order.getBuyer() != null && order.getBuyer().getUsername() != null
                && order.getBuyer().getUsername().toLowerCase().contains(kw)) {
            return true;
        }
        // Match seller usernames
        Set<String> sellers = getSellerUsernames(order);
        for (String seller : sellers) {
            if (seller.toLowerCase().contains(kw)) return true;
        }
        return false;
    }

    private Set<String> getSellerUsernames(Order order) {
        Set<String> sellers = new LinkedHashSet<>();
        if (order.getOrderItems() == null) return sellers;
        for (OrderItem item : order.getOrderItems()) {
            if (item.getSeller() != null && item.getSeller().getUsername() != null) {
                sellers.add(item.getSeller().getUsername());
            }
        }
        return sellers;
    }

    @Transactional(readOnly = true)
    public TransactionStatsDTO getTransactionStats() {
        long total = orderRepository.count();

        long pending = orderRepository.countByOrderStatusStatusName(OrderStatus.PENDING);
        long completed = orderRepository.countByOrderStatusStatusName(OrderStatus.COMPLETED);
        long cancelled = orderRepository.countByOrderStatusStatusName(OrderStatus.CANCELLED);
        long refunded = orderRepository.countByOrderStatusStatusName(OrderStatus.REFUNDED);
        long disputed = orderRepository.countByOrderStatusStatusName(OrderStatus.DISPUTED);

        long success = completed;
        long frozen = disputed;

        return new TransactionStatsDTO(total, success, pending, refunded, frozen,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Optional<TransactionDTO> getTransactionById(String transactionId) {
        if (transactionId == null || transactionId.isEmpty()) {
            return Optional.empty();
        }

        try {
            if (transactionId.startsWith("TXN-ORDER-")) {
                Long id = Long.parseLong(transactionId.substring(10));
                Optional<Order> o = orderRepository.findDetailedByOrderId(id);
                if (o.isPresent()) return Optional.of(mapOrder(o.get()));
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid transaction ID format: {}", transactionId);
        } catch (org.hibernate.LazyInitializationException e) {
            log.error("LazyInitializationException while loading transaction {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Failed to load transaction " + transactionId + ": " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    private TransactionDTO mapOrder(Order o) {
        User buyer = o.getBuyer();
        Set<String> sellerUsernames = getSellerUsernames(o);
        List<TransactionDTO.UserInfo> sellerInfos = sellerUsernames.stream()
                .map(username -> new TransactionDTO.UserInfo(null, username, null))
                .collect(Collectors.toList());

        return new TransactionDTO(
            "TXN-ORDER-" + o.getOrderId(),
            "ORDER_" + (o.getOrderStatus() != null ? o.getOrderStatus().getStatusName() : "PENDING"),
            "ORDER",
            o.getOrderStatus() != null ? o.getOrderStatus().getStatusName() : "PENDING",
            o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO,
            toUserInfo(buyer),
            sellerInfos.isEmpty() ? null : sellerInfos.get(0),
            toOrderInfo(o),
            o.getCreatedAt(),
            o.getUpdatedAt(),
            "Order: " + o.getOrderNumber()
        );
    }

    private TransactionDTO.UserInfo toUserInfo(User u) {
        if (u == null) return null;
        return new TransactionDTO.UserInfo(u.getUserId(), u.getUsername(), u.getEmail());
    }

    private TransactionDTO.OrderInfo toOrderInfo(Order o) {
        if (o == null) return null;
        int qty = o.getOrderItems() != null ? o.getOrderItems().size() : 0;
        return new TransactionDTO.OrderInfo(o.getOrderId(), o.getOrderNumber(), qty);
    }
}
