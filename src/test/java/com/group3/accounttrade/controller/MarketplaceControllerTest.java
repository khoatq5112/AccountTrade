package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.CategoryService;
import com.group3.accounttrade.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class MarketplaceControllerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private MarketplaceController marketplaceController;

    @Test
    void marketplaceDetailReturnsViewForExistingPost() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(marketplaceController).build();
        Post post = Post.builder().postId(7).title("Notion Pro").price(BigDecimal.valueOf(100000)).build();
        when(categoryService.getPostById(7)).thenReturn(post);
        when(postService.getPurchaseAvailability(post))
                .thenReturn(new PostService.PurchaseAvailability(true, null, 1));

        mockMvc.perform(get("/marketplace/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("marketplace_detail"))
                .andExpect(model().attribute("post", post))
                .andExpect(model().attribute("canPurchasePost", true))
                .andExpect(model().attribute("purchaseUnavailableMessage", nullValue()));
    }

    @Test
    void marketplaceDetailAddsUnavailableMessageWhenPostCannotBePurchased() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(marketplaceController).build();
        Post post = Post.builder().postId(7).title("Notion Pro").price(BigDecimal.valueOf(100000)).build();
        when(categoryService.getPostById(7)).thenReturn(post);
        when(postService.getPurchaseAvailability(post))
                .thenReturn(new PostService.PurchaseAvailability(false, "Sản phẩm đã hết tài khoản khả dụng.", 0));

        mockMvc.perform(get("/marketplace/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("marketplace_detail"))
                .andExpect(model().attribute("canPurchasePost", false))
                .andExpect(model().attribute("purchaseUnavailableMessage", "Sản phẩm đã hết tài khoản khả dụng."));
    }

    @Test
    void marketplaceDetailRedirectsForMissingPost() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(marketplaceController).build();
        when(categoryService.getPostById(8)).thenReturn(null);

        mockMvc.perform(get("/marketplace/8"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/marketplace?error=not_found"));
    }
}
