package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.PaymentStatus;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostCredential;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CartRepository;
import com.group3.accounttrade.repository.OrderItemRepository;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PaymentRepository;
import com.group3.accounttrade.repository.PaymentStatusRepository;
import com.group3.accounttrade.repository.PostRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private static final int DEFAULT_QUANTITY = 1;

    private final PostRepository postRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final CartRepository cartRepository;
    private final CredentialService credentialService;
    private final VnpayPaymentService vnpayPaymentService;

    @Transactional
    public CheckoutSession initiateCheckout(User buyer, Integer postId, HttpServletRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getSeller() != null && post.getSeller().getUserId().equals(buyer.getUserId())) {
            throw new IllegalArgumentException("You cannot purchase your own post");
        }

        if (!post.isInStock()) {
            throw new IllegalStateException("Product is out of stock");
        }

        if (!credentialService.hasEnoughCredentials(post, DEFAULT_QUANTITY)) {
            throw new IllegalStateException("No available credentials for this post");
        }

        OrderStatus awaitingPaymentStatus = orderStatusRepository.findByStatusName(OrderStatus.AWAITING_PAYMENT)
                .orElseThrow(() -> new IllegalStateException("AWAITING_PAYMENT status not found"));
        PaymentStatus initiatedStatus = paymentStatusRepository.findByStatusName(PaymentStatus.INITIATED)
                .orElseThrow(() -> new IllegalStateException("INITIATED payment status not found"));

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .buyer(buyer)
                .orderStatus(awaitingPaymentStatus)
                .subtotal(post.getPrice())
                .platformFee(BigDecimal.ZERO)
                .totalAmount(post.getPrice())
                .buyerIpAddress(request.getRemoteAddr())
                .buyerUserAgent(request.getHeader("User-Agent"))
                .build();
        order = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .post(post)
                .seller(post.getSeller())
                .unitPrice(post.getPrice())
                .platformFee(BigDecimal.ZERO)
                .sellerEarnings(post.getPrice())
                .postTitleSnapshot(post.getTitle())
                .itemStatus(OrderItem.STATUS_PENDING)
                .build();
        orderItem = orderItemRepository.save(orderItem);

        List<PostCredential> reservedCredentials = credentialService.reserveCredentials(post, DEFAULT_QUANTITY, orderItem);
        PostCredential reservedCredential = reservedCredentials.get(0);
        orderItem.setAssignedCredential(reservedCredential);
        orderItemRepository.save(orderItem);

        String txnRef = vnpayPaymentService.generateTxnRef(order);
        Payment payment = Payment.builder()
                .order(order)
                .paymentStatus(initiatedStatus)
                .vnpayTxnRef(txnRef)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .orderInfo("Thanh toan don hang " + order.getOrderNumber())
                .vnpayIpAddress(request.getRemoteAddr())
                .build();
        paymentRepository.save(payment);

        cartRepository.findByUserAndPost(buyer, post).ifPresent(cartRepository::delete);

        String paymentUrl = vnpayPaymentService.generatePaymentUrl(order, request, txnRef);
        return CheckoutSession.builder()
                .order(order)
                .payment(payment)
                .paymentUrl(paymentUrl)
                .build();
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    @Builder
    public record CheckoutSession(Order order, Payment payment, String paymentUrl) {
    }
}
