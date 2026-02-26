package com.group3.accounttrade.controller;

import com.group3.accounttrade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Passwords do not match");
            return "redirect:/register.html";
        }

        try {
            userService.registerUser(username, email, password, role);
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verify-otp";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/register.html";
        }
    }

    @GetMapping("/api/auth/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = userService.isEmailTaken(email);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String attemptVerifyOtp(@RequestParam String email, @RequestParam String otp,
            RedirectAttributes redirectAttributes) {
        boolean success = userService.activateUserWithOtp(email, otp);
        if (success) {
            return "redirect:/login.html?registered=true";
        } else {
            redirectAttributes.addAttribute("error", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verify-otp";
        }
    }
}
