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

    // Additional endpoints for admin actions can be defined here based on the
    // schema
    // e.g. managing users, resolving complaints, platform finances
}
