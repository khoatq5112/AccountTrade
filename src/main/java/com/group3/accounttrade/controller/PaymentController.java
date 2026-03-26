package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling VNPAY payment callbacks.
 * 
 * Key endpoints:
 * - /payment/vnpay/ipn - IPN (Instant Payment Notification) callback from VNPAY server
 * - /payment/vnpay/return - Return URL callback (browser redirect after payment)
 * 
 * IMPORTANT: 
 * - IPN is the TRUSTED source for payment confirmation
 * - Return URL should NOT be used to complete orders - only for UI display
 */
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final VnpayPaymentService vnpayPaymentService;
    private final OrderRepository orderRepository;

    /**
     * Handles VNPAY IPN (Instant Payment Notification) callback.
     * This is a server-to-server callback from VNPAY and should be TRUSTED.
     * 
     * IPN is called by VNPAY's servers, not the user's browser.
     * This is where we actually confirm payment and update order status.
     * 
     * @param request The HTTP request containing VNPAY callback parameters
     * @return JSON response with RspCode and Message
     */
    @PostMapping("/vnpay/ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleVnpayIpn(HttpServletRequest request) {
        log.info("Received VNPAY IPN callback");
        
        Map<String, String> params = extractVnpayParams(request);
        Map<String, String> response = vnpayPaymentService.processIpnCallback(params, request);
        
        log.info("VNPAY IPN response: {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * Alternative GET endpoint for IPN (some VNPAY versions use GET).
     */
    @GetMapping("/vnpay/ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleVnpayIpnGet(HttpServletRequest request) {
        return handleVnpayIpn(request);
    }

    /**
     * Handles VNPAY Return URL callback.
     * This is a browser redirect from VNPAY after payment completion.
     * 
     * IMPORTANT: This should NOT be used to confirm payment or complete orders.
     * Use this only to show payment status to the user.
     * The actual payment confirmation happens in the IPN handler.
     * 
     * @param request The HTTP request containing VNPAY callback parameters
     * @return ModelAndView showing payment result page
     */
    @GetMapping("/vnpay/return")
    public ModelAndView handleVnpayReturn(HttpServletRequest request) {
        log.info("Received VNPAY return callback");
        
        Map<String, String> params = extractVnpayParams(request);
        VnpayPaymentService.PaymentResult result = vnpayPaymentService.processReturnCallback(params, request);

        if (result.isTopUp()) {
            if (result.isSuccess() && result.getPendingPostId() != null) {
                ModelAndView mav = new ModelAndView("redirect:/buyer/checkout");
                mav.addObject("postId", result.getPendingPostId());
                return new ModelAndView("redirect:/buyer/checkout?postId=" + result.getPendingPostId() + "&topupSuccess=true");
            }
            ModelAndView mav = new ModelAndView("redirect:/buyer/wallet");
            if (result.isSuccess()) {
                return new ModelAndView("redirect:/buyer/wallet?topupSuccess=true");
            }
            return new ModelAndView("redirect:/buyer/wallet?topupFailed=true");
        }
        
        ModelAndView mav = new ModelAndView("payment_result");
        mav.addObject("success", result.isSuccess());
        mav.addObject("message", result.getMessage());
        mav.addObject("orderId", result.getOrderId());
        mav.addObject("orderSummary", buildOrderSummary(result.getOrderId()));
        
        // Add all VNPAY params for debugging (only in sandbox mode)
        mav.addObject("vnpParams", params);
        
        return mav;
    }

    /**
     * Displays payment status page for an order.
     * 
     * @param orderId The order ID
     * @return ModelAndView showing payment status
     */
    @GetMapping("/status/{orderId}")
    public ModelAndView getPaymentStatus(@PathVariable Long orderId) {
        ModelAndView mav = new ModelAndView("payment_status");
        mav.addObject("orderId", orderId);
        // Additional status information can be added here
        return mav;
    }

    /**
     * Extracts VNPAY parameters from the request.
     * Only extracts parameters that start with "vnp_" prefix.
     * 
     * @param request The HTTP request
     * @return Map of VNPAY parameters
     */
    private Map<String, String> extractVnpayParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            if (paramName.startsWith("vnp_")) {
                String paramValue = request.getParameter(paramName);
                params.put(paramName, paramValue);
            }
        }
        
        log.debug("Extracted VNPAY params: {}", params.keySet());
        return params;
    }

    private PaymentOrderSummary buildOrderSummary(Long orderId) {
        if (orderId == null) {
            return null;
        }

        return orderRepository.findDetailedByOrderId(orderId)
                .map(this::toOrderSummary)
                .orElse(null);
    }

    private PaymentOrderSummary toOrderSummary(Order order) {
        List<PaymentOrderItemSummary> items = order.getOrderItems().stream()
                .map(item -> new PaymentOrderItemSummary(
                        item.getOrderItemId(),
                        item.getPostTitleSnapshot(),
                        item.getUnitPrice(),
                        item.getItemStatus()))
                .toList();

        String nextSteps = resolveNextSteps(order);
        String supportMessage = resolveSupportMessage(order);

        return new PaymentOrderSummary(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getOrderStatus() != null ? order.getOrderStatus().getStatusName() : null,
                items,
                nextSteps,
                supportMessage);
    }

    private String resolveNextSteps(Order order) {
        String statusName = order.getOrderStatus() != null ? order.getOrderStatus().getStatusName() : null;
        if ("AWAITING_BUYER_CONFIRMATION".equalsIgnoreCase(statusName)) {
            return "Vao Lich su mua hang de xem credentials da duoc giao, kiem tra tai khoan va xac nhan da nhan hang neu mo ta dung.";
        }
        if ("AWAITING_PAYMENT".equalsIgnoreCase(statusName)) {
            return "Trang thai thanh toan dang duoc dong bo. Vui long doi them vai giay roi mo lai trang thai don hang neu credentials chua hien ra.";
        }
        if ("PAYMENT_FAILED".equalsIgnoreCase(statusName)) {
            return "Don hang chua duoc thanh toan thanh cong. Ban co the quay lai marketplace hoac thu thanh toan lai tu lich su mua hang.";
        }
        return "Mo trang thai don hang de theo doi tien trinh ban giao. TrustBridge se cap nhat lich su mua hang ngay khi payment va credential duoc dong bo xong.";
    }

    private String resolveSupportMessage(Order order) {
        String statusName = order.getOrderStatus() != null ? order.getOrderStatus().getStatusName() : null;
        if ("AWAITING_BUYER_CONFIRMATION".equalsIgnoreCase(statusName)) {
            return "Neu credentials khong dung nhu mo ta, hay mo don hang va tao khieu nai ngay trong lich su mua hang de TrustBridge giu escrow va ho tro ban.";
        }
        return "Can ho tro? Hay kiem tra trang thai don hang trong Lich su mua hang hoac lien he doi ngu TrustBridge neu can xac minh them ve payment.";
    }

    public record PaymentOrderSummary(
            Long orderId,
            String orderNumber,
            BigDecimal totalAmount,
            String currency,
            String orderStatus,
            List<PaymentOrderItemSummary> items,
            String nextSteps,
            String supportMessage) {}

    public record PaymentOrderItemSummary(
            Long orderItemId,
            String productTitle,
            BigDecimal unitPrice,
            String itemStatus) {}
}
