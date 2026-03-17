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

import java.util.LinkedHashMap;
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
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        String normalizedEmail = email == null ? "" : email.trim();
        Map<String, Object> payload = new LinkedHashMap<>();

        if (normalizedEmail.isEmpty()) {
            payload.put("exists", false);
            payload.put("valid", false);
            payload.put("message", "Email is required");
            return ResponseEntity.badRequest().body(payload);
        }

        boolean exists = userService.isEmailTaken(normalizedEmail);
        payload.put("exists", exists);
        payload.put("valid", true);
        payload.put("message", exists ? "Email already exists" : "Email is available");
        return ResponseEntity.ok(payload);
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

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            userService.sendPasswordResetOtp(email);
            redirectAttributes.addAttribute("email", email);
            redirectAttributes.addAttribute("sent", "true");
            return "redirect:/forgot-password";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String email, @RequestParam String otp,
            @RequestParam String newPassword, @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/reset-password";
        }
        boolean success = userService.resetPasswordWithOtp(email, otp, newPassword);
        if (success) {
            return "redirect:/login.html?reset=true";
        } else {
            redirectAttributes.addAttribute("error", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/reset-password";
        }
    }
}
