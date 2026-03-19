package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.StockStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
import com.group3.accounttrade.service.BuyerOrderService;
import com.group3.accounttrade.service.CartService;
import com.group3.accounttrade.service.OrderCheckoutService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
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
        when(postRepository.findById(5)).thenReturn(Optional.of(post));

        Model model = new ExtendedModelMap();

        String viewName = buyerController.checkout(5, model);

        assertEquals("checkout", viewName);
        assertEquals(post, model.getAttribute("post"));
        assertEquals(buyer, model.getAttribute("user"));
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
        when(orderCheckoutService.initiateCheckout(org.mockito.ArgumentMatchers.eq(buyer), org.mockito.ArgumentMatchers.eq(9), org.mockito.ArgumentMatchers.any()))
                .thenReturn(checkoutSession);

        mockMvc.perform(post("/buyer/checkout").param("postId", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_TxnRef=abc"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
