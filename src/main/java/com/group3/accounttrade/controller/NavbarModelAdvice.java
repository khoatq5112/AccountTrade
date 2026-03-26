package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.WalletService;
import com.group3.accounttrade.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class NavbarModelAdvice {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    @ModelAttribute
    public void addNavbarAttributes(Model model, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }

        userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
            model.addAttribute("currentUser", user);
            model.addAttribute("navbarWalletBalance", walletService.getBalance(user));
            model.addAttribute("navbarUnreadNotificationCount",
                    notificationService.getUnreadCount(user.getUserId()).getTotalCount());
            model.addAttribute("navbarRoleLabel", toRoleLabel(user));
            model.addAttribute("navbarHomePath", resolveHomePath(user));
            model.addAttribute("navbarWalletPath", resolveWalletPath(user));
            model.addAttribute("navbarUserInitials", buildInitials(user));
        });
    }

    private String toRoleLabel(User user) {
        String roleName = getRoleName(user);
        if ("Admin".equalsIgnoreCase(roleName)) {
            return "Quản trị viên";
        }
        if ("Seller".equalsIgnoreCase(roleName)) {
            return "Người bán";
        }
        return "Người mua";
    }

    private String resolveHomePath(User user) {
        String roleName = getRoleName(user);
        if ("Admin".equalsIgnoreCase(roleName)) {
            return "/admin/dashboard";
        }
        if ("Seller".equalsIgnoreCase(roleName)) {
            return "/seller/dashboard";
        }
        return "/buyer/dashboard";
    }

    private String resolveWalletPath(User user) {
        String roleName = getRoleName(user);
        if ("Admin".equalsIgnoreCase(roleName)) {
            return "/admin/wallets";
        }
        if ("Seller".equalsIgnoreCase(roleName)) {
            return "/seller/dashboard";
        }
        return "/buyer/wallet";
    }

    private String buildInitials(User user) {
        String username = user.getUsername();
        if (username == null || username.isBlank()) {
            return "TB";
        }

        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }

        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private String getRoleName(User user) {
        return user.getRole() != null ? user.getRole().getRoleName() : "";
    }
}
