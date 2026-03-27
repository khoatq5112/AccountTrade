package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.Notification;
import com.group3.accounttrade.entity.NotificationPreference;
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
import com.group3.accounttrade.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private static final int DEFAULT_QUANTITY = 1;
    private static final Set<String> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.AWAITING_PAYMENT,
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.CREDENTIAL_ASSIGNED,
            OrderStatus.AWAITING_BUYER_CONFIRMATION
    );

    private final PostRepository postRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final CartRepository cartRepository;
    private final CredentialService credentialService;
    private final VnpayPaymentService vnpayPaymentService;
    private final CommissionService commissionService;
    private final WalletService walletService;
    private final EscrowService escrowService;
    private final PostService postService;
    private final NotificationService notificationService;

    @Transactional
    public CheckoutSession initiateCheckout(User buyer, Integer postId, HttpServletRequest request) {
        Post post = validatePostForCheckout(buyer, postId);
        assertNoActiveOrderForPost(buyer, postId);

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

        BigDecimal effectiveRate = commissionService.getEffectiveRate(post.getCategory());
        BigDecimal itemFee = commissionService.calculateFee(post.getPrice(), effectiveRate);
        BigDecimal itemSellerEarnings = post.getPrice().subtract(itemFee);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .post(post)
                .seller(post.getSeller())
                .unitPrice(post.getPrice())
                .platformFee(itemFee)
                .sellerEarnings(itemSellerEarnings)
                .postTitleSnapshot(post.getTitle())
                .itemStatus(OrderItem.STATUS_PENDING)
                .build();
        orderItem = orderItemRepository.save(orderItem);
        addOrderItemToOrder(order, orderItem);

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
        notifyOrderCreated(order, post);

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

    public WalletCheckoutPreview previewWalletCheckout(User buyer, Integer postId) {
        Post post = validatePostForCheckout(buyer, postId);
        BigDecimal walletBalance = walletService.getBalance(buyer);
        BigDecimal deficit = walletService.getDeficit(buyer, post.getPrice());

        return WalletCheckoutPreview.builder()
                .post(post)
                .walletBalance(walletBalance)
                .totalAmount(post.getPrice())
                .deficitAmount(deficit)
                .suggestedTopUpAmount(walletService.getSuggestedTopUpAmount(deficit))
                .hasSufficientBalance(deficit.compareTo(BigDecimal.ZERO) == 0)
                .build();
    }

    @Transactional
    public Order initiateWalletCheckout(User buyer, Integer postId) {
        WalletCheckoutPreview preview = previewWalletCheckout(buyer, postId);
        Post post = preview.post();
        assertNoActiveOrderForPost(buyer, postId);

        if (!preview.hasSufficientBalance()) {
            throw new WalletService.InsufficientBalanceException(
                    "Số dư ví không đủ để thanh toán. Bạn cần nạp thêm "
                            + preview.deficitAmount().stripTrailingZeros().toPlainString() + " VND.");
        }

        OrderStatus paidStatus = orderStatusRepository.findByStatusName(OrderStatus.PAID)
                .orElseThrow(() -> new IllegalStateException("PAID status not found"));
        PaymentStatus paidPaymentStatus = paymentStatusRepository.findByStatusName(PaymentStatus.PAID)
                .orElseThrow(() -> new IllegalStateException("PAID payment status not found"));

        String orderNumber = generateOrderNumber();

        BigDecimal effectiveRate = commissionService.getEffectiveRate(post.getCategory());
        BigDecimal itemFee = commissionService.calculateFee(post.getPrice(), effectiveRate);
        BigDecimal itemSellerEarnings = post.getPrice().subtract(itemFee);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(buyer)
                .orderStatus(paidStatus)
                .subtotal(post.getPrice())
                .platformFee(itemFee)
                .totalAmount(post.getPrice())
                .paidAt(LocalDateTime.now())
                .build();
        order = orderRepository.saveAndFlush(order);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .post(post)
                .seller(post.getSeller())
                .unitPrice(post.getPrice())
                .platformFee(itemFee)
                .sellerEarnings(itemSellerEarnings)
                .postTitleSnapshot(post.getTitle())
                .itemStatus(OrderItem.STATUS_PENDING)
                .build();

        List<PostCredential> reservedCredentials = credentialService.reserveCredentials(post, DEFAULT_QUANTITY, orderItem);
        PostCredential reservedCredential = reservedCredentials.get(0);
        orderItem.setAssignedCredential(reservedCredential);
        orderItem = orderItemRepository.saveAndFlush(orderItem);
        addOrderItemToOrder(order, orderItem);

        walletService.deductBalance(buyer, post.getPrice(), orderNumber);

        String walletTxnRef = "WALLET-" + orderNumber;
        Payment payment = Payment.builder()
                .order(order)
                .paymentStatus(paidPaymentStatus)
                .vnpayTxnRef(walletTxnRef)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .orderInfo("Thanh toan bang vi " + orderNumber)
                .paidAt(LocalDateTime.now())
                .ipnProcessed(true)
                .build();
        paymentRepository.save(payment);
        notifyOrderCreated(order, post);
        notifyPaymentReceived(order, post);

        escrowService.createEscrow(order);
        credentialService.assignCredentialsToBuyer(order);

        cartRepository.findByUserAndPost(buyer, post).ifPresent(cartRepository::delete);

        return order;
    }

    private void assertNoActiveOrderForPost(User buyer, Integer postId) {
        List<Order> existingOrders = orderRepository.findByBuyerAndPostIdAndStatusIn(
                buyer,
                postId,
                List.copyOf(ACTIVE_ORDER_STATUSES));
        if (!existingOrders.isEmpty()) {
            throw new IllegalStateException("Bạn đã có một đơn hàng đang xử lý cho sản phẩm này.");
        }
    }

    private Post validatePostForCheckout(User buyer, Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getSeller() != null && post.getSeller().getUserId().equals(buyer.getUserId())) {
            throw new IllegalArgumentException("You cannot purchase your own post");
        }

        PostService.PurchaseAvailability purchaseAvailability = postService.getPurchaseAvailability(post);
        if (!purchaseAvailability.purchasable()) {
            throw new IllegalStateException(purchaseAvailability.failureReason());
        }

        return post;
    }

    private void addOrderItemToOrder(Order order, OrderItem orderItem) {
        if (order.getOrderItems().stream().noneMatch(existing -> existing == orderItem)) {
            order.getOrderItems().add(orderItem);
        }
    }

    private void notifyOrderCreated(Order order, Post post) {
        notificationService.createNotification(
                order.getBuyer(),
                Notification.TYPE_ORDER,
                NotificationPreference.CATEGORY_ORDER,
                "Đơn hàng " + order.getOrderNumber() + " đã được tạo",
                "Đơn hàng cho sản phẩm " + post.getTitle() + " đã được tạo thành công.",
                Notification.PRIORITY_NORMAL,
                "ORDER",
                order.getOrderId(),
                "/buyer/purchases?orderId=" + order.getOrderId()
        );

        if (post.getSeller() != null) {
            notificationService.createNotification(
                    post.getSeller(),
                    Notification.TYPE_ORDER,
                    NotificationPreference.CATEGORY_ORDER,
                    "Bạn có đơn hàng mới " + order.getOrderNumber(),
                    "Sản phẩm " + post.getTitle() + " vừa có đơn hàng mới.",
                    Notification.PRIORITY_NORMAL,
                    "ORDER",
                    order.getOrderId(),
                    "/seller/orders"
            );
        }
    }

    private void notifyPaymentReceived(Order order, Post post) {
        notificationService.createNotification(
                order.getBuyer(),
                Notification.TYPE_PAYMENT,
                NotificationPreference.CATEGORY_PAYMENT,
                "Thanh toán đơn " + order.getOrderNumber() + " thành công",
                "Thanh toán cho sản phẩm " + post.getTitle() + " đã được ghi nhận thành công.",
                Notification.PRIORITY_NORMAL,
                "ORDER",
                order.getOrderId(),
                "/buyer/purchases?orderId=" + order.getOrderId()
        );
    }

    @Builder
    public record CheckoutSession(Order order, Payment payment, String paymentUrl) {
    }

    @Builder
    public record WalletCheckoutPreview(
            Post post,
            BigDecimal walletBalance,
            BigDecimal totalAmount,
            BigDecimal deficitAmount,
            BigDecimal suggestedTopUpAmount,
            boolean hasSufficientBalance) {
    }
}
