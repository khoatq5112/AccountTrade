package com.group3.accounttrade.service;

import com.group3.accounttrade.dto.TransactionDTO;
import com.group3.accounttrade.dto.TransactionStatsDTO;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminTransactionService adminTransactionService;

    @Test
    void getAllTransactionsExcludesPaymentFailedOrders() {
        Order completed = order(1L, "ORD-1", OrderStatus.COMPLETED);

        when(orderRepository.findAllForAdminExcludingStatus(OrderStatus.PAYMENT_FAILED, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(completed), PageRequest.of(0, 20), 1));

        var page = adminTransactionService.getAllTransactions(null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        assertEquals("ORD-1", page.getContent().get(0).orderInfo().orderNumber());
    }

    @Test
    void getTransactionStatsSubtractsPaymentFailedOrdersFromTotal() {
        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.countByOrderStatusStatusName(OrderStatus.PAYMENT_FAILED)).thenReturn(2L);
        when(orderRepository.countByOrderStatusStatusName(OrderStatus.PENDING)).thenReturn(3L);
        when(orderRepository.countByOrderStatusStatusName(OrderStatus.COMPLETED)).thenReturn(4L);
        when(orderRepository.countByOrderStatusStatusName(OrderStatus.CANCELLED)).thenReturn(1L);
        when(orderRepository.countByOrderStatusStatusName(OrderStatus.REFUNDED)).thenReturn(0L);
        when(orderRepository.countByOrderStatusStatusName(OrderStatus.DISPUTED)).thenReturn(0L);

        TransactionStatsDTO stats = adminTransactionService.getTransactionStats();

        assertEquals(8L, stats.totalTransactions());
    }

    private Order order(Long id, String orderNumber, String statusName) {
        return Order.builder()
                .orderId(id)
                .orderNumber(orderNumber)
                .totalAmount(BigDecimal.valueOf(99_000))
                .createdAt(LocalDateTime.now())
                .orderStatus(OrderStatus.builder().statusName(statusName).build())
                .build();
    }
}
