package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.PaymentStatus;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostCredential;
import com.group3.accounttrade.entity.StockStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CartRepository;
import com.group3.accounttrade.repository.OrderItemRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PaymentRepository;
import com.group3.accounttrade.repository.PaymentStatusRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCheckoutServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusRepository paymentStatusRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CredentialService credentialService;

    @Mock
    private VnpayPaymentService vnpayPaymentService;

    @Mock
    private CommissionService commissionService;

    @Mock
    private WalletService walletService;

    @Mock
    private EscrowService escrowService;

    @Mock
    private PostService postService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderCheckoutService orderCheckoutService;

    @Test
    void initiateWalletCheckoutSucceedsWhenBuyerHasSufficientBalance() {
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        Category category = Category.builder().categoryId(7).categoryName("Learning").build();
        Post post = Post.builder()
                .postId(9)
                .title("Coursera Plus - 1 Year")
                .price(BigDecimal.valueOf(199_000))
                .seller(seller)
                .category(category)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
        PostCredential credential = PostCredential.builder().credentialId(77).post(post).build();
        OrderStatus paidStatus = OrderStatus.builder().statusName(OrderStatus.PAID).build();
        PaymentStatus paidPaymentStatus = PaymentStatus.builder().statusName(PaymentStatus.PAID).build();

        when(postRepository.findById(9)).thenReturn(Optional.of(post));
        when(postService.getPurchaseAvailability(post))
                .thenReturn(new PostService.PurchaseAvailability(true, null, 1));
        when(walletService.getBalance(buyer)).thenReturn(BigDecimal.valueOf(429_000));
        when(walletService.getDeficit(buyer, post.getPrice())).thenReturn(BigDecimal.ZERO);
        when(orderStatusRepository.findByStatusName(OrderStatus.PAID)).thenReturn(Optional.of(paidStatus));
        when(paymentStatusRepository.findByStatusName(PaymentStatus.PAID)).thenReturn(Optional.of(paidPaymentStatus));
        when(commissionService.getEffectiveRate(category)).thenReturn(BigDecimal.valueOf(5));
        when(commissionService.calculateFee(post.getPrice(), BigDecimal.valueOf(5))).thenReturn(BigDecimal.valueOf(9_950));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getOrderId() == null) {
                order.setOrderId(100L);
            }
            return order;
        });
        when(orderItemRepository.saveAndFlush(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            if (item.getOrderItemId() == null) {
                item.setOrderItemId(200L);
            }
            return item;
        });
        when(credentialService.reserveCredentials(eq(post), eq(1), any(OrderItem.class))).thenReturn(List.of(credential));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.findByUserAndPost(buyer, post)).thenReturn(Optional.empty());

        Order order = orderCheckoutService.initiateWalletCheckout(buyer, 9);

        assertEquals(1, order.getOrderItems().size());
        assertSame(credential, order.getOrderItems().get(0).getAssignedCredential());
        verify(walletService).deductBalance(eq(buyer), eq(post.getPrice()), eq(order.getOrderNumber()));
        verify(paymentRepository).save(any(Payment.class));
        verify(escrowService).createEscrow(argThat(savedOrder -> savedOrder.getOrderItems().size() == 1));
        verify(credentialService).assignCredentialsToBuyer(argThat(savedOrder -> savedOrder.getOrderItems().size() == 1));
        verify(cartRepository).findByUserAndPost(buyer, post);
        verifyNoInteractions(vnpayPaymentService);
    }

    @Test
    void initiateWalletCheckoutThrowsWhenBuyerHasInsufficientBalance() {
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        Post post = Post.builder()
                .postId(9)
                .title("Coursera Plus - 1 Year")
                .price(BigDecimal.valueOf(199_000))
                .seller(seller)
                .stockStatus(StockStatus.IN_STOCK)
                .build();

        when(postRepository.findById(9)).thenReturn(Optional.of(post));
        when(postService.getPurchaseAvailability(post))
                .thenReturn(new PostService.PurchaseAvailability(true, null, 1));
        when(walletService.getBalance(buyer)).thenReturn(BigDecimal.valueOf(20_000));
        when(walletService.getDeficit(buyer, post.getPrice())).thenReturn(BigDecimal.valueOf(179_000));
        when(walletService.getSuggestedTopUpAmount(BigDecimal.valueOf(179_000))).thenReturn(BigDecimal.valueOf(179_000));

        assertThrows(WalletService.InsufficientBalanceException.class,
                () -> orderCheckoutService.initiateWalletCheckout(buyer, 9));

        verify(walletService, never()).deductBalance(any(), any(), any());
        verify(paymentRepository, never()).save(any());
        verify(escrowService, never()).createEscrow(any());
        verify(credentialService, never()).assignCredentialsToBuyer(any());
        verifyNoInteractions(vnpayPaymentService);
    }

    @Test
    void initiateWalletCheckoutPropagatesFailureAfterDeduction() {
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        Category category = Category.builder().categoryId(7).categoryName("Learning").build();
        Post post = Post.builder()
                .postId(9)
                .title("Coursera Plus - 1 Year")
                .price(BigDecimal.valueOf(199_000))
                .seller(seller)
                .category(category)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
        PostCredential credential = PostCredential.builder().credentialId(77).post(post).build();
        OrderStatus paidStatus = OrderStatus.builder().statusName(OrderStatus.PAID).build();
        PaymentStatus paidPaymentStatus = PaymentStatus.builder().statusName(PaymentStatus.PAID).build();

        when(postRepository.findById(9)).thenReturn(Optional.of(post));
        when(postService.getPurchaseAvailability(post))
                .thenReturn(new PostService.PurchaseAvailability(true, null, 1));
        when(walletService.getBalance(buyer)).thenReturn(BigDecimal.valueOf(429_000));
        when(walletService.getDeficit(buyer, post.getPrice())).thenReturn(BigDecimal.ZERO);
        when(orderStatusRepository.findByStatusName(OrderStatus.PAID)).thenReturn(Optional.of(paidStatus));
        when(paymentStatusRepository.findByStatusName(PaymentStatus.PAID)).thenReturn(Optional.of(paidPaymentStatus));
        when(commissionService.getEffectiveRate(category)).thenReturn(BigDecimal.valueOf(5));
        when(commissionService.calculateFee(post.getPrice(), BigDecimal.valueOf(5))).thenReturn(BigDecimal.valueOf(9_950));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getOrderId() == null) {
                order.setOrderId(100L);
            }
            return order;
        });
        when(orderItemRepository.saveAndFlush(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            if (item.getOrderItemId() == null) {
                item.setOrderItemId(200L);
            }
            return item;
        });
        when(credentialService.reserveCredentials(eq(post), eq(1), any(OrderItem.class))).thenReturn(List.of(credential));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(escrowService.createEscrow(any(Order.class))).thenThrow(new RuntimeException("Escrow create failed"));

        assertThrows(RuntimeException.class, () -> orderCheckoutService.initiateWalletCheckout(buyer, 9));

        verify(walletService).deductBalance(eq(buyer), eq(post.getPrice()), any());
        verify(paymentRepository).save(any(Payment.class));
        verify(credentialService, never()).assignCredentialsToBuyer(any());
        verify(cartRepository, never()).findByUserAndPost(any(), any());
        verifyNoInteractions(vnpayPaymentService);
    }

    @Test
    void previewWalletCheckoutThrowsConcreteMessageWhenPostHasNoAvailableCredentials() {
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        Post post = Post.builder()
                .postId(9)
                .title("Coursera Plus - 1 Year")
                .price(BigDecimal.valueOf(199_000))
                .seller(seller)
                .stockStatus(StockStatus.IN_STOCK)
                .build();

        when(postRepository.findById(9)).thenReturn(Optional.of(post));
        when(postService.getPurchaseAvailability(post))
                .thenReturn(new PostService.PurchaseAvailability(false, "Sản phẩm đã hết tài khoản khả dụng.", 0));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderCheckoutService.previewWalletCheckout(buyer, 9));

        assertEquals("Sản phẩm đã hết tài khoản khả dụng.", exception.getMessage());
        verifyNoInteractions(walletService);
        verifyNoInteractions(vnpayPaymentService);
    }

    @Test
    void initiateCheckoutRejectsDuplicateActiveOrderForSamePost() {
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        Post post = Post.builder()
                .postId(9)
                .title("Coursera Plus - 1 Year")
                .price(BigDecimal.valueOf(199_000))
                .seller(seller)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
        Order existingOrder = Order.builder()
                .orderId(50L)
                .orderStatus(OrderStatus.builder().statusName(OrderStatus.AWAITING_PAYMENT).build())
                .build();

        when(postRepository.findById(9)).thenReturn(Optional.of(post));
        when(postService.getPurchaseAvailability(post))
                .thenReturn(new PostService.PurchaseAvailability(true, null, 1));
        when(orderRepository.findByBuyerAndPostIdAndStatusIn(eq(buyer), eq(9), any()))
                .thenReturn(List.of(existingOrder));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> orderCheckoutService.initiateCheckout(buyer, 9, null));

        assertEquals("Bạn đã có một đơn hàng đang xử lý cho sản phẩm này.", exception.getMessage());
        verifyNoInteractions(vnpayPaymentService);
        verify(paymentRepository, never()).save(any());
    }
}
