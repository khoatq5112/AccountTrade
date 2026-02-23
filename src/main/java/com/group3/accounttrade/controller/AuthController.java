package com.group3.accounttrade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String role) {

        // TODO: Validate password matching, strength, check if username/email exists
        // TODO: Hash the password
        // TODO: Save user to the database depending on the schema (Users table with
        // role_id)

        System.out.println("Registering user: " + username + " with role: " + role);

        // For now, simply redirect to login page after successful "mock" registration
        return "redirect:/login.html?registered=true";
    }
}
