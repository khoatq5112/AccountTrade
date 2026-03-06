package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Cart;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostStatus;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.CartRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.PostStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final PostRepository postRepository;
    private final PostStatusRepository postStatusRepository;

    @Transactional
    public Cart addToCart(User user, Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Check if post is available
        PostStatus availableStatus = postStatusRepository.findByStatusName("Available")
                .orElseThrow(() -> new RuntimeException("Status not found"));

        if (!post.getStatus().getStatusName().equals("Available")) {
            throw new RuntimeException("Post is not available");
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
        return cartRepository.findByUser(user);
    }

    @Transactional
    public void removeFromCart(User user, Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        cartRepository.deleteByUserAndPost(user, post);
    }

    @Transactional
    public void clearCart(User user) {
        List<Cart> carts = cartRepository.findByUser(user);
        cartRepository.deleteAll(carts);
    }
}
