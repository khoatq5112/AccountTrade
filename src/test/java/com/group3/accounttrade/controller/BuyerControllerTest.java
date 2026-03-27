package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.StockStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
import com.group3.accounttrade.service.BuyerOrderService;
import com.group3.accounttrade.service.CartService;
import com.group3.accounttrade.service.CloudinaryService;
import com.group3.accounttrade.service.DisputeService;
import com.group3.accounttrade.service.OrderCheckoutService;
import com.group3.accounttrade.service.WalletService;
import com.group3.accounttrade.util.IdEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class BuyerControllerTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CartService cartService;

    @Mock
    private BuyerOrderService buyerOrderService;

    @Mock
    private OrderCheckoutService orderCheckoutService;

    @Mock
    private WalletService walletService;

    @Mock
    private DisputeService disputeService;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private IdEncoder idEncoder;

    @InjectMocks
    private BuyerController buyerController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cartRedirectsAnonymousUsersToLogin() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();

        mockMvc.perform(get("/buyer/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html?redirect=/buyer/cart"));
    }

    @Test
    void checkoutPageRendersForAvailablePost() throws Exception {
        User buyer = User.builder().userId(1).username("buyer").build();
        User seller = User.builder().userId(2).username("seller").build();
        Post post = Post.builder()
                .postId(5)
                .title("Figma Pro")
                .price(BigDecimal.valueOf(150000))
                .seller(seller)
                .stockStatus(StockStatus.IN_STOCK)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(orderCheckoutService.previewWalletCheckout(buyer, 5))
                .thenReturn(OrderCheckoutService.WalletCheckoutPreview.builder()
                        .post(post)
                        .walletBalance(BigDecimal.valueOf(200000))
                        .totalAmount(post.getPrice())
                        .deficitAmount(BigDecimal.ZERO)
                        .suggestedTopUpAmount(BigDecimal.ZERO)
                        .hasSufficientBalance(true)
                        .build());
        when(walletService.getMinimumTopUpAmount()).thenReturn(BigDecimal.valueOf(20000));
        when(walletService.getMaximumTopUpAmount()).thenReturn(BigDecimal.valueOf(5000000));
        when(walletService.getPresetTopUpAmounts()).thenReturn(List.of(
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(200000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(2000000)
        ));
        when(idEncoder.decodePostId("5")).thenReturn(5);
        when(idEncoder.encodePostId(5)).thenReturn("post_5token");

        Model model = new ExtendedModelMap();

        String viewName = buyerController.checkout("5", null, null, null, model);

        assertEquals("checkout", viewName);
        assertEquals(post, model.getAttribute("post"));
        assertEquals(buyer, model.getAttribute("user"));
    }

    @Test
    void checkoutRedirectsBackToMarketplaceWhenPostIsUnavailable() {
        User buyer = User.builder().userId(1).username("buyer").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(orderCheckoutService.previewWalletCheckout(buyer, 5))
                .thenThrow(new IllegalStateException("Sản phẩm đã hết tài khoản khả dụng."));
        when(idEncoder.decodePostId("5")).thenReturn(5);
        when(idEncoder.encodePostId(5)).thenReturn("post_5token");

        Model model = new ExtendedModelMap();

        String viewName = buyerController.checkout("5", null, null, null, model);

        assertEquals("redirect:/marketplace/post_5token?error=unavailable", viewName);
    }

    @Test
    void checkoutPostRedirectsToVnpayAfterSuccess() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();
        User buyer = User.builder().userId(1).username("buyer").build();
        OrderCheckoutService.CheckoutSession checkoutSession = OrderCheckoutService.CheckoutSession.builder()
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=abc")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(idEncoder.decodePostId("9")).thenReturn(9);
        when(idEncoder.encodePostId(9)).thenReturn("post_9token");
        when(orderCheckoutService.initiateCheckout(org.mockito.ArgumentMatchers.eq(buyer), org.mockito.ArgumentMatchers.eq(9), org.mockito.ArgumentMatchers.any()))
                .thenReturn(checkoutSession);

        mockMvc.perform(post("/buyer/checkout").param("postId", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=abc"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void walletCheckoutRedirectsToPurchasesAfterSuccess() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();
        User buyer = User.builder().userId(1).username("buyer").build();
        Order order = Order.builder().orderId(12L).orderNumber("ORD-12").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(idEncoder.decodePostId("9")).thenReturn(9);
        when(idEncoder.encodePostId(9)).thenReturn("post_9token");
        when(orderCheckoutService.initiateWalletCheckout(buyer, 9)).thenReturn(order);

        mockMvc.perform(post("/buyer/checkout")
                        .param("postId", "9")
                        .param("paymentMethod", "WALLET"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/purchases?orderId=12"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void walletCheckoutRedirectsBackToCheckoutWhenBalanceIsInsufficient() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();
        User buyer = User.builder().userId(1).username("buyer").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(idEncoder.decodePostId("9")).thenReturn(9);
        when(idEncoder.encodePostId(9)).thenReturn("post_9token");
        when(orderCheckoutService.initiateWalletCheckout(buyer, 9))
                .thenThrow(new WalletService.InsufficientBalanceException("Số dư không đủ."));

        mockMvc.perform(post("/buyer/checkout")
                        .param("postId", "9")
                        .param("paymentMethod", "WALLET"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/checkout?postId=post_9token"))
                .andExpect(flash().attribute("errorMessage", "Số dư không đủ."));
    }

    @Test
    void walletCheckoutShowsGenericMessageWhenUnexpectedExceptionOccurs() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();
        User buyer = User.builder().userId(1).username("buyer").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(idEncoder.decodePostId("9")).thenReturn(9);
        when(idEncoder.encodePostId(9)).thenReturn("post_9token");
        when(orderCheckoutService.initiateWalletCheckout(buyer, 9))
                .thenThrow(new RuntimeException("database exploded"));

        mockMvc.perform(post("/buyer/checkout")
                        .param("postId", "9")
                        .param("paymentMethod", "WALLET"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/checkout?postId=post_9token"))
                .andExpect(flash().attribute("errorMessage", "Thanh toán bằng ví thất bại. Vui lòng thử lại."));
    }

    @Test
    void checkoutRejectsInvalidPaymentMethod() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();
        User buyer = User.builder().userId(1).username("buyer").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(idEncoder.decodePostId("9")).thenReturn(9);
        when(idEncoder.encodePostId(9)).thenReturn("post_9token");

        mockMvc.perform(post("/buyer/checkout")
                        .param("postId", "9")
                        .param("paymentMethod", "BITCOIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/marketplace/post_9token"))
                .andExpect(flash().attribute("errorMessage", "Phương thức thanh toán không hợp lệ."));
    }

    @Test
    void topUpValidationErrorRedirectsBackToCheckoutWhenPurchaseIsPending() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();
        User buyer = User.builder().userId(1).username("buyer").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(idEncoder.decodePostId("9")).thenReturn(9);
        doThrow(new IllegalArgumentException("Số tiền nạp tối thiểu là 20,000 ₫."))
                .when(walletService)
                .initiateTopUp(eq(buyer),
                        eq(BigDecimal.valueOf(5000)),
                        isNull(),
                        eq(9),
                        any());

        mockMvc.perform(post("/buyer/wallet/topup")
                        .param("amount", "5000")
                        .param("pendingPostId", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/checkout?postId=9"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void openDisputeRejectsTooLongDescriptionBeforeUploadingEvidence() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(buyerController).build();
        User buyer = User.builder().userId(1).username("buyer").build();
        String longDescription = "a".repeat(2001);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));

        mockMvc.perform(post("/buyer/orders/55/disputes")
                        .param("reason", DisputeService.REASON_INVALID_CREDENTIAL)
                        .param("description", longDescription))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/purchases?orderId=55"))
                .andExpect(flash().attribute("errorMessage", "Mô tả khiếu nại không được vượt quá 2000 ký tự."));
    }

    @Test
    void purchasesHideExpiredOrdersUnlessExplicitlyHighlighted() {
        User buyer = User.builder().userId(1).username("buyer").build();
        Order expiredOrder = Order.builder()
                .orderId(44L)
                .createdAt(LocalDateTime.now().minusHours(2))
                .orderStatus(com.group3.accounttrade.entity.OrderStatus.builder()
                        .statusName(com.group3.accounttrade.entity.OrderStatus.PAYMENT_EXPIRED)
                        .build())
                .build();
        Order activeOrder = Order.builder()
                .orderId(45L)
                .createdAt(LocalDateTime.now().minusHours(1))
                .orderStatus(com.group3.accounttrade.entity.OrderStatus.builder()
                        .statusName(com.group3.accounttrade.entity.OrderStatus.AWAITING_PAYMENT)
                        .build())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(walletRepository.findByUser_UserId(1)).thenReturn(Optional.empty());
        when(orderRepository.findByBuyer(buyer)).thenReturn(List.of(expiredOrder, activeOrder));

        Model hiddenModel = new ExtendedModelMap();
        String hiddenView = buyerController.viewPurchases(null, null, null, hiddenModel, null);

        assertEquals("buyer_purchases", hiddenView);
        List<Order> hiddenOrders = (List<Order>) hiddenModel.getAttribute("workflowOrders");
        assertEquals(1, hiddenOrders.size());
        assertEquals(45L, hiddenOrders.get(0).getOrderId());

        Model highlightedModel = new ExtendedModelMap();
        String highlightedView = buyerController.viewPurchases(44L, null, null, highlightedModel, null);

        assertEquals("buyer_purchases", highlightedView);
        List<Order> highlightedOrders = (List<Order>) highlightedModel.getAttribute("workflowOrders");
        assertEquals(2, highlightedOrders.size());
        assertTrue(highlightedOrders.stream().anyMatch(order -> order.getOrderId().equals(44L)));
        assertTrue(highlightedOrders.stream().anyMatch(order -> order.getOrderId().equals(45L)));
    }

    @Test
    void purchasesHidePaymentFailedOrders() {
        User buyer = User.builder().userId(1).username("buyer").build();
        Order failedOrder = Order.builder()
                .orderId(46L)
                .createdAt(LocalDateTime.now().minusHours(2))
                .orderStatus(com.group3.accounttrade.entity.OrderStatus.builder()
                        .statusName(com.group3.accounttrade.entity.OrderStatus.PAYMENT_FAILED)
                        .build())
                .build();
        Order activeOrder = Order.builder()
                .orderId(47L)
                .createdAt(LocalDateTime.now().minusHours(1))
                .orderStatus(com.group3.accounttrade.entity.OrderStatus.builder()
                        .statusName(com.group3.accounttrade.entity.OrderStatus.AWAITING_PAYMENT)
                        .build())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("buyer", "pw", List.of()));
        when(userRepository.findByUsername("buyer")).thenReturn(Optional.of(buyer));
        when(walletRepository.findByUser_UserId(1)).thenReturn(Optional.empty());
        when(orderRepository.findByBuyer(buyer)).thenReturn(List.of(failedOrder, activeOrder));

        Model model = new ExtendedModelMap();
        String view = buyerController.viewPurchases(null, null, null, model, null);

        assertEquals("buyer_purchases", view);
        List<Order> orders = (List<Order>) model.getAttribute("workflowOrders");
        assertEquals(1, orders.size());
        assertEquals(47L, orders.get(0).getOrderId());
    }
}
