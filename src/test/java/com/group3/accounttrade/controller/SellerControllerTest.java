package com.group3.accounttrade.controller;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void createPostAcceptsMultipleCredentials() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(sellerController).build();

        MockMultipartFile thumbnailFile =
                new MockMultipartFile("thumbnailFile", "thumb.jpg", "image/jpeg", new byte[] {1, 2, 3});

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

    @Test
    void createPostRejectsMissingThumbnail() throws Exception {
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
                        .param("credentials[0].accountPassword", "123456"))
                .andExpect(status().isOk());
    }

    @Test
    void createPostAcceptsDescriptionContainingOnlyImage() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(sellerController).build();

        MockMultipartFile thumbnailFile =
                new MockMultipartFile("thumbnailFile", "thumb.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/seller/posts")
                        .file(thumbnailFile)
                        .principal(new UsernamePasswordAuthenticationToken("sellerAccount", "pw", List.of()))
                        .param("title", "Netflix Premium")
                        .param("price", "300000")
                        .param("categoryId", "1")
                        .param("description", "<p><img src=\"https://example.com/image.jpg\"></p>")
                        .param("credentials[0].accountUsername", "abcde")
                        .param("credentials[0].accountPassword", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/posts"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(postService).createPost(any(), eq("sellerAccount"));
    }
}
