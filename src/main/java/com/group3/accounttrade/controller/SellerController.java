package com.group3.accounttrade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller")
public class SellerController {

    @GetMapping("/dashboard")
    public String viewSellerDashboard() {
        return "seller_dashboard";
    }

    // Additional endpoints for seller actions can be defined here based on the
    // schema
    // e.g. managing posts, viewing transactions, wallet
}
