package com.group3.accounttrade.controller;

import com.group3.accounttrade.config.CustomAuthenticationFailureHandler;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String viewHomePage() {
        return "index";
    }

    @GetMapping("/login.html")
    public String viewLoginPageHtml(HttpSession session, Model model) {
        // Restore the username typed before a failed login attempt
        String lastUsername = (String) session.getAttribute(CustomAuthenticationFailureHandler.LAST_USERNAME_KEY);
        if (lastUsername != null) {
            model.addAttribute("lastUsername", lastUsername);
            session.removeAttribute(CustomAuthenticationFailureHandler.LAST_USERNAME_KEY); // consume once
        }
        return "login";
    }

    @GetMapping("/index.html")
    public String viewIndexPageHtml() {
        return "index";
    }

    @GetMapping("/register.html")
    public String viewRegisterPageHtml() {
        return "register";
    }
}
