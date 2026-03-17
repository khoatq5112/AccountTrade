package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Cart;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CartRepository;
import com.group3.accounttrade.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final PostRepository postRepository;

    @Transactional
    public Cart addToCart(User user, Integer postId) {
        validateBuyerAccess(user);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.isInStock()) {
            throw new RuntimeException("Post is not available");
        }

        if (post.getSeller() != null && post.getSeller().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("You cannot add your own post to cart");
        }

        // Check if already in cart
        if (cartRepository.existsByUserAndPost(user, post)) {
            throw new RuntimeException("Post already in cart");
        }

        Cart cart = Cart.builder()
                .user(user)
                .post(post)
                .build();

        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public List<Cart> getUserCart(User user) {
        validateBuyerAccess(user);
        return cartRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<CartItemDto> getUserCartItems(User user) {
        validateBuyerAccess(user);
        return cartRepository.findByUser(user).stream()
                .map(this::toCartItemDto)
                .toList();
    }

    @Transactional
    public void removeFromCart(User user, Integer postId) {
        validateBuyerAccess(user);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        cartRepository.deleteByUserAndPost(user, post);
    }

    @Transactional
    public void clearCart(User user) {
        validateBuyerAccess(user);
        List<Cart> carts = cartRepository.findByUser(user);
        cartRepository.deleteAll(carts);
    }

    @Transactional(readOnly = true)
    public int getCartItemCount(User user) {
        validateBuyerAccess(user);
        return cartRepository.findByUser(user).size();
    }

    private void validateBuyerAccess(User user) {
        if (user == null || user.getRole() == null || user.getRole().getRoleName() == null) {
            throw new RuntimeException("User role is invalid");
        }

        if ("Seller".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new RuntimeException("Seller accounts cannot use the cart");
        }
    }

    private CartItemDto toCartItemDto(Cart cart) {
        Post post = cart.getPost();
        String sellerName = post.getSeller() != null ? post.getSeller().getUsername() : "Unknown seller";
        String thumbnailUrl = post.getResolvedThumbnailUrl();
        BigDecimal price = post.getPrice();
        LocalDateTime addedAt = cart.getCreatedAt();

        return new CartItemDto(
                post.getPostId(),
                post.getTitle(),
                thumbnailUrl,
                price,
                sellerName,
                addedAt);
    }

    public record CartItemDto(
            Integer postId,
            String title,
            String thumbnailUrl,
            BigDecimal price,
            String sellerName,
            LocalDateTime addedAt) {
    }
}
