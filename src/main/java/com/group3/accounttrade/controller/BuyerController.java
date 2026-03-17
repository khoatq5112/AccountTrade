package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.Transaction;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.BuyerTransactionService;
import com.group3.accounttrade.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final BuyerTransactionService buyerTransactionService;

    @GetMapping("/dashboard")
    public String viewBuyerDashboard() {
        return "buyer_dashboard";
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam Integer postId, Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/checkout?postId=" + postId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace/" + postId + "?error=seller_restricted";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return "redirect:/marketplace?error=not_found";
        }

        if (post.getSeller() != null && post.getSeller().getUserId().equals(user.getUserId())) {
            return "redirect:/marketplace/" + postId + "?error=own_post";
        }

        if (!post.isInStock()) {
            return "redirect:/marketplace/" + postId + "?error=unavailable";
        }

        model.addAttribute("post", post);
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        return "checkout";
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/cart";
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        List<CartService.CartItemDto> cartItems = cartService.getUserCartItems(user);
        BigDecimal subtotal = cartItems.stream()
                .map(CartService.CartItemDto::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("cartCount", cartItems.size());
        model.addAttribute("isAuthenticated", true);
        return "cart";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam Integer postId, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/checkout?postId=" + postId;
        }
        if (isSellerAccount(user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản seller không thể mua sản phẩm.");
            return "redirect:/marketplace/" + postId;
        }

        try {
            Transaction transaction = buyerTransactionService.checkoutPost(user, postId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đơn hàng đã được tạo và đang chờ TrustBridge xử lý trung gian.");
            return "redirect:/buyer/purchases?transactionId=" + transaction.getTransactionId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/marketplace/" + postId;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/marketplace/" + postId;
        }
    }

    @GetMapping("/purchases")
    public String viewPurchases(@RequestParam(required = false) Integer transactionId, Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/purchases";
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        List<Transaction> purchases = buyerTransactionService.getPurchases(user);
        model.addAttribute("purchases", purchases);
        model.addAttribute("highlightTransactionId", transactionId);
        model.addAttribute("isAuthenticated", true);
        return "buyer_purchases";
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean isSellerAccount(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && "Seller".equalsIgnoreCase(user.getRole().getRoleName());
    }
}
