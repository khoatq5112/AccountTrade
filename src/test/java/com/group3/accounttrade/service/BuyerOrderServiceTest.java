package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuyerOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private BuyerOrderService buyerOrderService;

    @Test
    void dashboardSummaryUsesOrdersInsteadOfLegacyTransactions() {
        User buyer = User.builder().userId(7).username("buyer").build();

        Order awaitingPayment = order(OrderStatus.AWAITING_PAYMENT, BigDecimal.valueOf(100_000), LocalDateTime.now().minusDays(3));
        Order awaitingConfirmation = order(OrderStatus.AWAITING_BUYER_CONFIRMATION, BigDecimal.valueOf(200_000), LocalDateTime.now().minusDays(2));
        Order disputed = order(OrderStatus.DISPUTED, BigDecimal.valueOf(300_000), LocalDateTime.now().minusDays(1));
        Order completed = order(OrderStatus.COMPLETED, BigDecimal.valueOf(400_000), LocalDateTime.now());

        when(orderRepository.findByBuyer(buyer)).thenReturn(List.of(awaitingPayment, awaitingConfirmation, disputed, completed));

        BuyerOrderService.BuyerDashboardSummary summary = buyerOrderService.getDashboardSummary(buyer);

        assertEquals(BigDecimal.valueOf(500_000), summary.escrowAmount());
        assertEquals(2, summary.escrowCount());
        assertEquals(BigDecimal.valueOf(400_000), summary.totalSpent());
        assertEquals(1, summary.completedCount());
        assertEquals(4, summary.orders().size());
        assertEquals(3, summary.pendingOrders().size());
    }

    private Order order(String statusName, BigDecimal totalAmount, LocalDateTime createdAt) {
        return Order.builder()
                .orderId((long) createdAt.getDayOfMonth())
                .totalAmount(totalAmount)
                .createdAt(createdAt)
                .orderStatus(OrderStatus.builder().statusName(statusName).build())
                .build();
    }
}
