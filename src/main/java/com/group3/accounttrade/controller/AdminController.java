package com.group3.accounttrade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /**
     * Displays the dispute detail page for admin intervention.
     * The dispute data is loaded via JavaScript from the REST API.
     *
     * @param id The dispute ID
     * @param model The model to pass the dispute ID to the view
     * @return The admin_dispute_detail template
     */
    @GetMapping("/disputes/{id}")
    public String viewAdminDisputeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("disputeId", id);
        return "admin_dispute_detail";
    }
}
