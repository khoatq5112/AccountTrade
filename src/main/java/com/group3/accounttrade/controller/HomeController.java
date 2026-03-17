package com.group3.accounttrade.controller;

import com.group3.accounttrade.config.CustomAuthenticationFailureHandler;
import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String viewHomePage(Model model) {
        // Load all categories
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        // Load posts by category for homepage sections
        model.addAttribute("entertainmentPosts", categoryService.getPostsByCategoryName("Giải trí", 4));
        model.addAttribute("musicPosts", categoryService.getPostsByCategoryName("Nghe Nhạc", 4));
        model.addAttribute("moviePosts", categoryService.getPostsByCategoryName("Xem Phim", 4));
        attachAuthState(model);

        return "index";
    }

    @GetMapping({"/login", "/login.html"})
    public String viewLoginPage(HttpSession session, Model model) {
        String lastUsername = (String) session.getAttribute(CustomAuthenticationFailureHandler.LAST_USERNAME_KEY);
        if (lastUsername != null) {
            model.addAttribute("lastUsername", lastUsername);
            session.removeAttribute(CustomAuthenticationFailureHandler.LAST_USERNAME_KEY);
        }
        return "login";
    }

    @GetMapping("/index.html")
    public String viewIndexPageHtml() {
        return "redirect:/";
    }

    @GetMapping({"/register", "/register.html"})
    public String viewRegisterPageHtml() {
        return "register";
    }

    private void attachAuthState(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (!isAuthenticated) {
            return;
        }

        User currentUser = userRepository.findByUsername(auth.getName())
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null));

        if (currentUser != null) {
            model.addAttribute("currentUsername", currentUser.getUsername());
            model.addAttribute("currentRole", currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "");
        }
    }
}
