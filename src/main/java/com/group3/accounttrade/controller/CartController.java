package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Cart;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.CartService;
import com.group3.accounttrade.util.IdEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final IdEncoder idEncoder;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam String postId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Please login first"));
        }

        try {
            Integer decodedPostId = idEncoder.decodePostId(postId);
            Cart cart = cartService.addToCart(user, decodedPostId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Added to cart successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCartItems() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Please login first"));
        }

        List<CartService.CartItemDto> cartItems = cartService.getUserCartItems(user);
        return ResponseEntity.ok(cartItems);
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCartCount() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Please login first"));
        }

        // Sellers don't have a cart, return 0
        if (user.getRole() != null && "Seller".equalsIgnoreCase(user.getRole().getRoleName())) {
            return ResponseEntity.ok(Map.of("count", 0));
        }

        return ResponseEntity.ok(Map.of("count", cartService.getCartItemCount(user)));
    }

    @DeleteMapping("/remove/{postId}")
    public ResponseEntity<?> removeFromCart(@PathVariable String postId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Please login first"));
        }

        try {
            Integer decodedPostId = idEncoder.decodePostId(postId);
            cartService.removeFromCart(user, decodedPostId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Removed from cart successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Please login first"));
        }

        cartService.clearCart(user);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Cart cleared successfully"
        ));
    }
}
