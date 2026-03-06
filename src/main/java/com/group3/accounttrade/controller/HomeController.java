package com.group3.accounttrade.controller;

import com.group3.accounttrade.config.CustomAuthenticationFailureHandler;
import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.service.CategoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategoryService categoryService;

    @GetMapping("/")
    public String viewHomePage(Model model) {
        // Load parent categories with subcategories
        List<Category> parentCategories = categoryService.getAllParentCategories();
        model.addAttribute("parentCategories", parentCategories);

        // Load posts by category for homepage sections
        model.addAttribute("googleDrivePosts", categoryService.getPostsByCategoryName("Google Drive", 4));
        model.addAttribute("vpnPosts", categoryService.getPostsByCategoryName("VPN, Bảo mật mạng", 4));
        model.addAttribute("aiPosts", categoryService.getPostsByCategoryName("Thế giới AI", 4));
        model.addAttribute("giftCardPosts", categoryService.getPostsByCategoryName("Gift Card", 4));
        model.addAttribute("designPosts", categoryService.getPostsByCategoryName("Edit Ảnh - Video", 4));
        model.addAttribute("microsoftPosts", categoryService.getPostsByCategoryName("Windows, Office", 4));
        model.addAttribute("steamPosts", categoryService.getPostsByCategoryName("Steam Wallet", 4));
        model.addAttribute("entertainmentPosts", categoryService.getPostsByCategoryName("Giải trí", 4));
        model.addAttribute("workPosts", categoryService.getPostsByCategoryName("Làm việc", 4));
        model.addAttribute("learningPosts", categoryService.getPostsByCategoryName("Học tập", 4));

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
}
