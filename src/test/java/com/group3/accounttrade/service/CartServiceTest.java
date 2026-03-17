package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Cart;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.StockStatus;
import com.group3.accounttrade.entity.Role;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CartRepository;
import com.group3.accounttrade.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CartService cartService;

    private User buyer;
    private User seller;
    private Post availablePost;

    @BeforeEach
    void setUp() {
        Role buyerRole = Role.builder().roleId(1).roleName("Buyer").build();
        Role sellerRole = Role.builder().roleId(2).roleName("Seller").build();

        buyer = User.builder().userId(1).username("buyer").role(buyerRole).build();
        seller = User.builder().userId(2).username("seller").role(sellerRole).build();

        availablePost = Post.builder()
                .postId(10)
                .title("Netflix Premium")
                .price(BigDecimal.valueOf(120000))
                .seller(seller)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
    }

    @Test
    void addToCartRejectsOwnPost() {
        Post ownPost = Post.builder()
                .postId(12)
                .seller(buyer)
                .stockStatus(StockStatus.IN_STOCK)
                .build();
        when(postRepository.findById(12)).thenReturn(Optional.of(ownPost));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> cartService.addToCart(buyer, 12));

        assertEquals("You cannot add your own post to cart", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addToCartRejectsUnavailablePost() {
        Post unavailablePost = Post.builder()
                .postId(13)
                .seller(seller)
                .stockStatus(StockStatus.OUT_OF_STOCK)
                .build();
        when(postRepository.findById(13)).thenReturn(Optional.of(unavailablePost));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> cartService.addToCart(buyer, 13));

        assertEquals("Post is not available", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addToCartRejectsDuplicatePost() {
        when(postRepository.findById(10)).thenReturn(Optional.of(availablePost));
        when(cartRepository.existsByUserAndPost(buyer, availablePost)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> cartService.addToCart(buyer, 10));

        assertEquals("Post already in cart", exception.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getUserCartItemsReturnsDtoSafePayload() {
        Cart cart = Cart.builder().cartId(3).user(buyer).post(availablePost).build();
        when(cartRepository.findByUser(buyer)).thenReturn(List.of(cart));

        List<CartService.CartItemDto> items = cartService.getUserCartItems(buyer);

        assertEquals(1, items.size());
        assertEquals(availablePost.getPostId(), items.get(0).postId());
        assertEquals(availablePost.getTitle(), items.get(0).title());
        assertEquals(seller.getUsername(), items.get(0).sellerName());
    }
}
