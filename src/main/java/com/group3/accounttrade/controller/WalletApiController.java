package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.OrderCheckoutService;
import com.group3.accounttrade.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletApiController {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final OrderCheckoutService orderCheckoutService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getWalletSummary() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("balance", walletService.getBalance(user));
        response.put("minimumTopUpAmount", walletService.getMinimumTopUpAmount());
        response.put("maximumTopUpAmount", walletService.getMaximumTopUpAmount());
        response.put("topUpPresetAmounts", walletService.getPresetTopUpAmounts());
        response.put("transactionHistory", walletService.getTransactionHistory(user).stream()
                .map(item -> {
                    Map<String, Object> transaction = new LinkedHashMap<>();
                    transaction.put("transactionId", item.getTransactionId());
                    transaction.put("type", item.getType());
                    transaction.put("amount", item.getAmount());
                    transaction.put("balanceAfter", item.getBalanceAfter());
                    transaction.put("description", item.getDescription());
                    transaction.put("referenceId", item.getReferenceId());
                    transaction.put("createdAt", item.getCreatedAt());
                    return transaction;
                })
                .toList());
        response.put("topUpHistory", walletService.getTopUpHistory(user).stream()
                .map(item -> {
                    Map<String, Object> topUp = new LinkedHashMap<>();
                    topUp.put("topUpId", item.getTopUpId());
                    topUp.put("amount", item.getAmount());
                    topUp.put("requiredAmount", item.getRequiredAmount());
                    topUp.put("status", item.getStatus());
                    topUp.put("vnpayTxnRef", item.getVnpayTxnRef());
                    topUp.put("pendingPostId", item.getPendingPostId());
                    topUp.put("createdAt", item.getCreatedAt());
                    return topUp;
                })
                .toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/checkout-preview")
    public ResponseEntity<Map<String, Object>> getCheckoutPreview(@RequestParam Integer postId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        OrderCheckoutService.WalletCheckoutPreview preview = orderCheckoutService.previewWalletCheckout(user, postId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("postId", preview.post().getPostId());
        response.put("postTitle", preview.post().getTitle());
        response.put("price", preview.totalAmount());
        response.put("walletBalance", preview.walletBalance());
        response.put("deficitAmount", preview.deficitAmount());
        response.put("suggestedTopUpAmount", preview.suggestedTopUpAmount());
        response.put("minimumTopUpAmount", walletService.getMinimumTopUpAmount());
        response.put("maximumTopUpAmount", walletService.getMaximumTopUpAmount());
        response.put("topUpPresetAmounts", walletService.getPresetTopUpAmounts());
        response.put("hasSufficientBalance", preview.hasSufficientBalance());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> createTopUp(@RequestParam java.math.BigDecimal amount,
                                                           @RequestParam(required = false) java.math.BigDecimal requiredAmount,
                                                           @RequestParam(required = false) Integer pendingPostId,
                                                           HttpServletRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        String paymentUrl = walletService.initiateTopUp(user, amount, requiredAmount, pendingPostId, request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("paymentUrl", paymentUrl);
        response.put("pendingPostId", pendingPostId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchaseWithWallet(@RequestParam Integer postId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        try {
            Order order = orderCheckoutService.initiateWalletCheckout(user, postId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("orderId", order.getOrderId());
            response.put("orderNumber", order.getOrderNumber());
            response.put("redirectUrl", "/buyer/purchases?orderId=" + order.getOrderId());
            return ResponseEntity.ok(response);
        } catch (WalletService.InsufficientBalanceException ex) {
            OrderCheckoutService.WalletCheckoutPreview preview = orderCheckoutService.previewWalletCheckout(user, postId);
            Map<String, Object> response = errorBody(ex.getMessage());
            response.put("deficitAmount", preview.deficitAmount());
            response.put("suggestedTopUpAmount", preview.suggestedTopUpAmount());
            response.put("pendingPostId", postId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
