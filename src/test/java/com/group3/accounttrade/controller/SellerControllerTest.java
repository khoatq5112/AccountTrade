package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.entity.Wallet;
import com.group3.accounttrade.repository.CategoryRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
import com.group3.accounttrade.service.PostService;
import com.group3.accounttrade.service.SellerDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class SellerControllerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PostService postService;

    @Mock
    private SellerDashboardService sellerDashboardService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private SellerController sellerController;

    @Test
    void dashboardProvidesUnifiedSellerSidebarContext() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(sellerController).build();
        User seller = User.builder().username("sellerAccount").build();
        SellerDashboardService.SellerDashboardStats stats = new SellerDashboardService.SellerDashboardStats(
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                0L,
                0L,
                0L,
                0L,
                0L,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO);

        when(sellerDashboardService.getDashboardStats("sellerAccount")).thenReturn(stats);
        when(sellerDashboardService.getPendingOrders("sellerAccount", 5)).thenReturn(List.of());
        when(userRepository.findByUsername("sellerAccount")).thenReturn(Optional.of(seller));
        when(walletRepository.findByUser_Username("sellerAccount")).thenReturn(Optional.of(Wallet.builder().build()));

        mockMvc.perform(get("/seller/dashboard")
                        .principal(new UsernamePasswordAuthenticationToken("sellerAccount", "pw", List.of())))
                .andExpect(status().isOk())
                .andExpect(view().name("seller_dashboard"))
                .andExpect(model().attribute("sellerUsername", "sellerAccount"))
                .andExpect(model().attribute("sellerBadgeText", "Đã xác minh cấp 2"))
                .andExpect(model().attribute("activeSellerNav", "dashboard"));
    }

    @Test
    void postsProvidesUnifiedSellerSidebarContext() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(sellerController).build();
        User seller = User.builder().username("sellerAccount").build();

        when(postService.getSellerPostsWithFilters(eq("sellerAccount"), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(categoryRepository.findAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(userRepository.findByUsername("sellerAccount")).thenReturn(Optional.of(seller));

        mockMvc.perform(get("/seller/posts")
                        .principal(new UsernamePasswordAuthenticationToken("sellerAccount", "pw", List.of())))
                .andExpect(status().isOk())
                .andExpect(view().name("seller_posts"))
                .andExpect(model().attribute("sellerUsername", "sellerAccount"))
                .andExpect(model().attribute("sellerBadgeText", "Đã xác minh cấp 2"))
                .andExpect(model().attribute("activeSellerNav", "posts"));
    }

    @Test
    void createPostAcceptsMultipleCredentials() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(sellerController).build();

        MockMultipartFile thumbnailFile =
                new MockMultipartFile("thumbnailFile", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/seller/posts")
                        .file(thumbnailFile)
                        .principal(new UsernamePasswordAuthenticationToken("sellerAccount", "pw", List.of()))
                        .param("title", "Duolingo Max")
                        .param("price", "300000")
                        .param("categoryId", "1")
                        .param("description", "<p>Mo ta</p>")
                        .param("credentials[0].accountUsername", "abcde")
                        .param("credentials[0].accountPassword", "123456")
                        .param("credentials[0].securityNotes", "Profile 1")
                        .param("credentials[1].accountUsername", "khoa411")
                        .param("credentials[1].accountPassword", "654321")
                        .param("credentials[1].securityNotes", "Profile 2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/posts"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(postService).createPost(any(), eq("sellerAccount"));
    }
}
