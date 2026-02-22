package com.group3.accounttrade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String viewHomePage() {
        return "index";
    }

    @GetMapping("/login.html")
    public String viewLoginPageHtml() {
        return "login";
    }

    @GetMapping("/index.html")
    public String viewIndexPageHtml() {
        return "index";
    }
}
