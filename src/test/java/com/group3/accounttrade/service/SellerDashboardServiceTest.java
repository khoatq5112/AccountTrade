package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.EscrowRepository;
import com.group3.accounttrade.repository.OrderItemRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerDashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SellerDashboardService sellerDashboardService;

    @Test
    void getSellerOrdersHidesPaymentFailedRows() {
        User seller = User.builder().userId(2).username("seller").build();
        OrderItem failedItem = orderItem("ORD-FAILED", OrderStatus.PAYMENT_FAILED);
        OrderItem completedItem = orderItem("ORD-OK", OrderStatus.COMPLETED);

        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
        when(orderItemRepository.findSellerOrderItems(seller, null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(failedItem, completedItem), PageRequest.of(0, 10), 2));

        var page = sellerDashboardService.getSellerOrders("seller", null, null, PageRequest.of(0, 10));

        assertEquals(1, page.getContent().size());
        assertEquals("ORD-OK", page.getContent().get(0).orderNumber());
    }

    private OrderItem orderItem(String orderNumber, String statusName) {
        Order order = Order.builder()
                .orderId(1L)
                .orderNumber(orderNumber)
                .createdAt(LocalDateTime.now())
                .orderStatus(OrderStatus.builder().statusName(statusName).build())
                .build();

        return OrderItem.builder()
                .order(order)
                .postTitleSnapshot("Skillshare Premium")
                .unitPrice(BigDecimal.valueOf(99_000))
                .build();
    }
}
