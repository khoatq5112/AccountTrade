package com.group3.accounttrade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public String viewAdminDashboard() {
        return "admin_dashboard";
    }

    @GetMapping("/users")
    public String viewAdminUsers() {
        return "admin_users";
    }

    @GetMapping("/posts")
    public String viewAdminPosts() {
        return "admin_posts";
    }

    @GetMapping("/wallets")
    public String viewAdminWallets() {
        return "admin_wallets";
    }

    @GetMapping("/transactions")
    public String viewAdminTransactions() {
        return "admin_transactions";
    }

    @GetMapping("/disputes")
    public String viewAdminDisputes() {
        return "admin_disputes";
    }
}
