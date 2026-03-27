package com.group3.accounttrade.service;

import com.group3.accounttrade.config.VnpayConfig;
import com.group3.accounttrade.entity.NotificationPreference;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.PaymentStatus;
import com.group3.accounttrade.entity.PaymentCallback;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.AuditLogRepository;
import com.group3.accounttrade.repository.EscrowRepository;
import com.group3.accounttrade.repository.EscrowStatusRepository;
import com.group3.accounttrade.repository.EscrowTransactionRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PaymentCallbackRepository;
import com.group3.accounttrade.repository.PaymentRepository;
import com.group3.accounttrade.repository.PaymentStatusRepository;
import com.group3.accounttrade.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnpayPaymentServiceTest {

    @Mock
    private VnpayConfig vnpayConfig;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentCallbackRepository paymentCallbackRepository;

    @Mock
    private PaymentStatusRepository paymentStatusRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private EscrowRepository escrowRepository;

    @Mock
    private EscrowStatusRepository escrowStatusRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CredentialService credentialService;

    @Mock
    private WalletService walletService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VnpayPaymentService vnpayPaymentService;

    @Test
    void processReturnCallbackConfirmsTopUpBeforeRedirectingBackToCheckout() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TU-12345");
        params.put("vnp_TransactionNo", "999888");
        params.put("vnp_ResponseCode", VnpayConfig.RESPONSE_SUCCESS);
        params.put("vnp_TransactionStatus", VnpayConfig.TXN_STATUS_SUCCESS);
        params.put("vnp_Amount", "21000000");
        params.put("vnp_SecureHash", "hash");

        when(request.getQueryString()).thenReturn("vnp_TxnRef=TU-12345");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(vnpayConfig.validateChecksum(params)).thenReturn(true);
        when(vnpayConfig.getClientIpAddress(request)).thenReturn("127.0.0.1");
        when(paymentCallbackRepository.save(any(PaymentCallback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.getPendingPostIdForTopUp("TU-12345")).thenReturn(12);

        VnpayPaymentService.PaymentResult result = vnpayPaymentService.processReturnCallback(params, request);

        assertTrue(result.isSuccess());
        assertTrue(result.isTopUp());
        assertEquals(12, result.getPendingPostId());
        verify(walletService).confirmTopUp("TU-12345", "999888");
    }

    @Test
    void expireStaleAwaitingPaymentsMarksOrderExpiredAndReleasesReservation() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 27, 10, 0);
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        OrderStatus awaiting = OrderStatus.builder().statusName(OrderStatus.AWAITING_PAYMENT).build();
        OrderStatus expired = OrderStatus.builder().statusName(OrderStatus.PAYMENT_EXPIRED).build();
        PaymentStatus initiated = PaymentStatus.builder().statusName(PaymentStatus.INITIATED).build();
        PaymentStatus failed = PaymentStatus.builder().statusName(PaymentStatus.FAILED).build();
        Order order = Order.builder()
                .orderId(22L)
                .orderNumber("ORD-22")
                .buyer(buyer)
                .orderStatus(awaiting)
                .createdAt(now.minusMinutes(16))
                .orderItems(List.of(OrderItem.builder().seller(seller).build()))
                .build();
        Payment payment = Payment.builder()
                .paymentId(99L)
                .order(order)
                .paymentStatus(initiated)
                .build();

        when(vnpayConfig.getTimeoutMinutes()).thenReturn(15);
        when(orderRepository.findExpiredAwaitingPaymentOrders(now.minusMinutes(15), OrderStatus.AWAITING_PAYMENT))
                .thenReturn(List.of(order));
        when(orderStatusRepository.findByStatusName(OrderStatus.PAYMENT_EXPIRED)).thenReturn(Optional.of(expired));
        when(paymentStatusRepository.findByStatusName(PaymentStatus.FAILED)).thenReturn(Optional.of(failed));
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.of(payment));

        vnpayPaymentService.expireStaleAwaitingPayments(now);

        assertEquals(OrderStatus.PAYMENT_EXPIRED, order.getOrderStatus().getStatusName());
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus().getStatusName());
        verify(credentialService).releaseOrderCredentials(order);
        verify(notificationService).createNotification(
                eq(buyer),
                eq("PAYMENT"),
                eq(NotificationPreference.CATEGORY_PAYMENT),
                any(),
                any(),
                anyInt(),
                eq("ORDER"),
                eq(order.getOrderId()),
                any());
        verify(notificationService).createNotification(
                eq(seller),
                eq("ORDER"),
                eq(NotificationPreference.CATEGORY_ORDER),
                any(),
                any(),
                anyInt(),
                eq("ORDER"),
                eq(order.getOrderId()),
                any());
    }

    @Test
    void expireStaleAwaitingPaymentsSkipsRecentOrders() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 27, 10, 0);
        when(vnpayConfig.getTimeoutMinutes()).thenReturn(15);
        when(orderRepository.findExpiredAwaitingPaymentOrders(now.minusMinutes(15), OrderStatus.AWAITING_PAYMENT))
                .thenReturn(List.of());

        vnpayPaymentService.expireStaleAwaitingPayments(now);

        verify(paymentRepository, never()).findByOrder(any());
        verify(credentialService, never()).releaseOrderCredentials(any());
    }
}
