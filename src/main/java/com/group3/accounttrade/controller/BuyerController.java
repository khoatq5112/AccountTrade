package com.group3.accounttrade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/buyer")
public class BuyerController {

    @GetMapping("/dashboard")
    public String viewBuyerDashboard() {
        return "buyer_dashboard";
    }

    // Additional endpoints for buyer actions can be defined here based on the
    // schema
    // e.g. purchasing posts, viewing transaction history
}
