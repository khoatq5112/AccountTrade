package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String viewBuyerDashboard() {
        return "buyer_dashboard";
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam Integer postId, Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/marketplace";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return "redirect:/marketplace?error=not_found";
        }

        model.addAttribute("post", post);
        model.addAttribute("user", user);
        return "checkout";
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        return "cart";
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
