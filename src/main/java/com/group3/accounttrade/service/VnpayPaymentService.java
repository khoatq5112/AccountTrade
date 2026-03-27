package com.group3.accounttrade.service;

import com.group3.accounttrade.config.VnpayConfig;
import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import com.group3.accounttrade.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for handling VNPAY payment operations.
 * 
 * Key responsibilities:
 * - Generating payment URLs for VNPAY redirect
 * - Processing IPN (Instant Payment Notification) callbacks
 * - Processing return URL callbacks
 * - Validating payment checksums
 * - Handling idempotent payment processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayPaymentService {

    private final VnpayConfig vnpayConfig;
    private final PaymentRepository paymentRepository;
    private final PaymentCallbackRepository paymentCallbackRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final EscrowRepository escrowRepository;
    private final EscrowStatusRepository escrowStatusRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final CredentialService credentialService;
    private final WalletService walletService;
    private final NotificationService notificationService;

    /**
     * Generates the VNPAY payment URL for a given order.
     *
     * @param order  The order to create payment for
     * @param request The HTTP request to get client IP
     * @return The VNPAY payment URL
     */
    public String generatePaymentUrl(Order order, jakarta.servlet.http.HttpServletRequest request) {
        String txnRef = generateTxnRef(order);
        return generatePaymentUrl(order, request, txnRef);
    }

    public String generatePaymentUrl(Order order, jakarta.servlet.http.HttpServletRequest request, String txnRef) {
        if (!vnpayConfig.isConfigured()) {
            throw new IllegalStateException("VNPAY is not properly configured");
        }

        String orderInfo = String.format("Thanh toan don hang %s", order.getOrderNumber());
        String amount = vnpayConfig.formatAmountForVnpay(order.getTotalAmount().doubleValue());
        String ipAddress = vnpayConfig.getClientIpAddress(request);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpayConfig.getVersion());
        vnpParams.put("vnp_Command", VnpayConfig.COMMAND_PAY);
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", amount);
        vnpParams.put("vnp_CurrCode", VnpayConfig.CURRENCY_VND);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", VnpayConfig.LOCALE_VN);
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", vnpayConfig.generateTimestamp());
        vnpParams.put("vnp_ExpireDate", vnpayConfig.generateExpireTimestamp());

        // Generate secure hash
        String secureHash = vnpayConfig.generateSecureHash(vnpParams);
        vnpParams.put("vnp_SecureHash", secureHash);

        // Build the payment URL
        StringBuilder paymentUrl = new StringBuilder(vnpayConfig.getPayUrl());
        paymentUrl.append("?");
        
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (i > 0) {
                    paymentUrl.append("&");
                }
                paymentUrl.append(fieldName)
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
            }
        }

        log.info("Generated VNPAY payment URL for order: {}, txnRef: {}", order.getOrderNumber(), txnRef);
        return paymentUrl.toString();
    }

    public String generateTxnRef(Order order) {
        return vnpayConfig.generateTxnRef(order.getOrderNumber());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireStaleAwaitingPayments() {
        expireStaleAwaitingPayments(LocalDateTime.now());
    }

    protected void expireStaleAwaitingPayments(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(vnpayConfig.getTimeoutMinutes());
        List<Order> expiredOrders = orderRepository.findExpiredAwaitingPaymentOrders(
                cutoff,
                OrderStatus.AWAITING_PAYMENT);

        if (expiredOrders.isEmpty()) {
            return;
        }

        OrderStatus paymentExpiredStatus = orderStatusRepository.findByStatusName(OrderStatus.PAYMENT_EXPIRED)
                .orElseThrow(() -> new IllegalStateException("PAYMENT_EXPIRED order status not found"));
        PaymentStatus failedPaymentStatus = paymentStatusRepository.findByStatusName(PaymentStatus.FAILED)
                .orElseThrow(() -> new IllegalStateException("FAILED payment status not found"));

        for (Order order : expiredOrders) {
            expireAwaitingPaymentOrder(order, paymentExpiredStatus, failedPaymentStatus, now);
        }
    }

    /**
     * Processes an IPN (Instant Payment Notification) callback from VNPAY.
     * This is the server-to-server callback and should be trusted over return URL.
     *
     * @param params  The callback parameters
     * @param request The HTTP request
     * @return Response map with status
     */
    @Transactional
    public Map<String, String> processIpnCallback(Map<String, String> params,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.info("Processing VNPAY IPN callback: {}", params);
        
        Map<String, String> response = new HashMap<>();
        
        // 1. Log the callback
        PaymentCallback callback = logCallback(params, "IPN", request);
        
        // 2. Validate checksum
        if (!vnpayConfig.validateChecksum(params)) {
            log.warn("IPN callback checksum validation failed");
            callback.setProcessed(true);
            callback.setProcessingResult("Checksum validation failed");
            paymentCallbackRepository.save(callback);
            
            response.put("RspCode", "97");
            response.put("Message", "Invalid checksum");
            return response;
        }
        
        // 3. Extract key parameters
        String txnRef = params.get("vnp_TxnRef");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String amountStr = params.get("vnp_Amount");

        // 3a. Handle wallet top-up IPN (TU- prefix)
        if (txnRef != null && txnRef.startsWith("TU-")) {
            boolean isSuccess = VnpayConfig.RESPONSE_SUCCESS.equals(responseCode) &&
                                VnpayConfig.TXN_STATUS_SUCCESS.equals(transactionStatus);
            if (isSuccess) {
                try {
                    walletService.confirmTopUp(txnRef, vnpTransactionNo);
                    log.info("Top-up IPN confirmed: {}", txnRef);
                } catch (Exception e) {
                    log.error("Error confirming top-up via IPN: {}", e.getMessage(), e);
                }
            } else {
                walletService.failTopUp(txnRef);
                log.info("Top-up IPN failed: {}, responseCode: {}", txnRef, responseCode);
            }
            callback.setProcessed(true);
            callback.setProcessingResult("Top-up IPN processed");
            paymentCallbackRepository.save(callback);
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;
        }

        // 4. Find the payment by txnRef
        Optional<Payment> paymentOpt = paymentRepository.findLockedByVnpayTxnRef(txnRef);
        if (paymentOpt.isEmpty()) {
            log.warn("IPN callback: Payment not found for txnRef: {}", txnRef);
            callback.setProcessed(true);
            callback.setProcessingResult("Payment not found");
            paymentCallbackRepository.save(callback);
            
            response.put("RspCode", "01");
            response.put("Message", "Order not found");
            return response;
        }
        
        Payment payment = paymentOpt.get();
        callback.setPayment(payment);
        
        // 5. Check for duplicate processing (idempotency)
        if (payment.getPaidAt() != null ||
            payment.getPaymentStatus().getStatusName().equals(PaymentStatus.PAID)) {
            log.info("IPN callback: Payment already processed: {}", txnRef);
            callback.setProcessed(true);
            callback.setProcessingResult("Already processed - idempotent success");
            paymentCallbackRepository.save(callback);
            
            // Return success for idempotency
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;
        }
        
        // 6. Update payment with VNPAY transaction details
        payment.setVnpayTransactionNo(vnpTransactionNo);
        payment.setVnpayResponseCode(responseCode);
        // Store transaction status in failure reason if not success
        if (!VnpayConfig.TXN_STATUS_SUCCESS.equals(transactionStatus)) {
            payment.setErrorMessage("TransactionStatus: " + transactionStatus);
        }
        payment.setBankCode(params.get("vnp_BankCode"));
        payment.setCardType(params.get("vnp_CardType"));
        payment.setOrderInfo(params.get("vnp_OrderInfo"));
        payment.setSecureHash(params.get("vnp_SecureHash"));
        
        // 7. Verify amount matches
        double callbackAmount = vnpayConfig.parseAmountFromVnpay(amountStr);
        if (Math.abs(callbackAmount - payment.getAmount().doubleValue()) > 0.01) {
            log.warn("IPN callback: Amount mismatch. Expected: {}, Got: {}", 
                    payment.getAmount(), callbackAmount);
            handlePaymentMismatch(payment, callback, "Amount mismatch");
            
            response.put("RspCode", "04");
            response.put("Message", "Invalid amount");
            return response;
        }
        
        // 8. Process based on response code
        if (VnpayConfig.RESPONSE_SUCCESS.equals(responseCode) && 
            VnpayConfig.TXN_STATUS_SUCCESS.equals(transactionStatus)) {
            handlePaymentSuccess(payment, callback);
        } else {
            handlePaymentFailure(payment, callback, responseCode, transactionStatus);
        }
        
        callback.setProcessed(true);
        callback.setProcessedAt(LocalDateTime.now());
        paymentCallbackRepository.save(callback);
        
        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    /**
     * Processes a return URL callback from VNPAY.
     * This is the browser redirect and should NOT be trusted for order completion.
     * Use this only to update UI and show status to user.
     *
     * @param params  The callback parameters
     * @param request The HTTP request
     * @return PaymentResult with status information
     */
    @Transactional
    public PaymentResult processReturnCallback(Map<String, String> params,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.info("Processing VNPAY return callback: {}", params);
        
        // 1. Log the callback
        PaymentCallback callback = logCallback(params, "RETURN", request);
        
        // 2. Validate checksum
        if (!vnpayConfig.validateChecksum(params)) {
            log.warn("Return callback checksum validation failed");
            callback.setProcessed(true);
            callback.setProcessingResult("Checksum validation failed");
            paymentCallbackRepository.save(callback);
            
            return PaymentResult.failed("Checksum validation failed", null);
        }
        
        // 3. Extract key parameters
        String txnRef = params.get("vnp_TxnRef");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        // 3a. Handle wallet top-up return (TU- prefix)
        if (txnRef != null && txnRef.startsWith("TU-")) {
            boolean callbackSuccess = VnpayConfig.RESPONSE_SUCCESS.equals(responseCode) &&
                    VnpayConfig.TXN_STATUS_SUCCESS.equals(transactionStatus);
            boolean sandboxReturnConfirmed = false;
            if (callbackSuccess && vnpayConfig.isSandboxMode()) {
                try {
                    walletService.confirmTopUp(txnRef, vnpTransactionNo);
                    log.info("Sandbox return callback confirmed top-up before IPN: {}", txnRef);
                    sandboxReturnConfirmed = true;
                } catch (Exception e) {
                    log.warn("Sandbox return callback could not confirm top-up {} yet: {}", txnRef, e.getMessage());
                }
            }

            String topUpStatus = walletService.getTopUpStatus(txnRef);
            callback.setProcessed(true);
            callback.setProcessingResult("Top-up return callback observed with status: " + topUpStatus);
            callback.setProcessedAt(LocalDateTime.now());
            paymentCallbackRepository.save(callback);

            Integer pendingPostId = walletService.getPendingPostIdForTopUp(txnRef);
            if (WalletTopUp.STATUS_COMPLETED.equals(topUpStatus) && pendingPostId != null) {
                if (sandboxReturnConfirmed) {
                    return PaymentResult.topUpSandboxReturnConfirmedWithRedirect(
                            "Nạp tiền thành công ngay trên callback RETURN của sandbox.", pendingPostId);
                }
                return PaymentResult.topUpSuccessWithRedirect(
                        "Nạp tiền thành công! Bạn có thể tiếp tục mua hàng.", pendingPostId);
            }
            if (WalletTopUp.STATUS_COMPLETED.equals(topUpStatus)) {
                if (sandboxReturnConfirmed) {
                    return PaymentResult.topUpSandboxReturnConfirmed(
                            "Nạp tiền thành công ngay trên callback RETURN của sandbox.");
                }
                return PaymentResult.topUpSuccess("Nạp tiền vào ví thành công!");
            }
            if (WalletTopUp.STATUS_FAILED.equals(topUpStatus) || !callbackSuccess) {
                return PaymentResult.failed("Nạp tiền thất bại: " + responseCode, null);
            }
            return PaymentResult.topUpPending("Giao dịch nạp tiền đang chờ xác nhận từ VNPAY.", pendingPostId);
        }

        // 4. Find the payment
        Optional<Payment> paymentOpt = paymentRepository.findByVnpayTxnRef(txnRef);
        if (paymentOpt.isEmpty()) {
            log.warn("Return callback: Payment not found for txnRef: {}", txnRef);
            callback.setProcessed(true);
            callback.setProcessingResult("Payment not found");
            paymentCallbackRepository.save(callback);
            
            return PaymentResult.failed("Payment not found", null);
        }
        
        Payment payment = paymentOpt.get();
        callback.setPayment(payment);
        callback.setProcessed(true);
        callback.setProcessingResult("Return callback processed");
        paymentCallbackRepository.save(callback);
        
        // 5. Return result based on current payment status (not updating - that's IPN's job)
        // But we can show optimistic status for UI purposes
        boolean isSuccess = VnpayConfig.RESPONSE_SUCCESS.equals(responseCode) && 
                           VnpayConfig.TXN_STATUS_SUCCESS.equals(transactionStatus);
        
        if (isSuccess) {
            return PaymentResult.success("Payment successful", payment.getOrder().getOrderId());
        } else {
            return PaymentResult.failed("Payment failed: " + responseCode, payment.getOrder().getOrderId());
        }
    }

    /**
     * Logs a payment callback to the database.
     */
    private PaymentCallback logCallback(Map<String, String> params, String callbackType,
            jakarta.servlet.http.HttpServletRequest request) {
        
        PaymentCallback callback = new PaymentCallback();
        callback.setCallbackType(callbackType);
        callback.setRawQueryString(request.getQueryString());
        callback.setVnpayTxnRef(params.get("vnp_TxnRef"));
        callback.setVnpayTransactionNo(params.get("vnp_TransactionNo"));
        callback.setVnpayResponseCode(params.get("vnp_ResponseCode"));
        // Store transaction status in processing result
        String txnStatus = params.get("vnp_TransactionStatus");
        callback.setAmount(params.get("vnp_Amount") != null ? Long.parseLong(params.get("vnp_Amount")) : null);
        callback.setReceivedHash(params.get("vnp_SecureHash"));
        callback.setSourceIp(vnpayConfig.getClientIpAddress(request));
        callback.setUserAgent(request.getHeader("User-Agent"));
        callback.setProcessed(false);
        if (txnStatus != null) {
            callback.setProcessingResult("TxnStatus: " + txnStatus);
        }
        
        return paymentCallbackRepository.save(callback);
    }

    /**
     * Handles successful payment processing.
     */
    @Transactional
    protected void handlePaymentSuccess(Payment payment, PaymentCallback callback) {
        log.info("Processing successful payment for order: {}", payment.getOrder().getOrderId());
        
        // 1. Update payment status
        PaymentStatus paidStatus = paymentStatusRepository.findByStatusName(PaymentStatus.PAID)
                .orElseThrow(() -> new IllegalStateException("PAID status not found"));
        payment.setPaymentStatus(paidStatus);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        // 2. Update order status
        Order order = payment.getOrder();
        OrderStatus paidOrderStatus = orderStatusRepository.findByStatusName(OrderStatus.PAID)
                .orElseThrow(() -> new IllegalStateException("PAID order status not found"));
        order.setOrderStatus(paidOrderStatus);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // 3. Create escrow record
        createEscrowForOrder(order);

        // 4. Assign and deliver the reserved credentials after payment confirmation
        credentialService.assignCredentialsToBuyer(order);
        notificationService.createNotification(
                order.getBuyer(),
                Notification.TYPE_PAYMENT,
                NotificationPreference.CATEGORY_PAYMENT,
                "Thanh toán đơn " + order.getOrderNumber() + " thành công",
                "Hệ thống đã ghi nhận thanh toán thành công cho đơn hàng của bạn.",
                Notification.PRIORITY_NORMAL,
                "PAYMENT",
                payment.getPaymentId(),
                "/buyer/purchases?orderId=" + order.getOrderId()
        );

        // 5. Create audit log
        createAuditLog(null, "PAYMENT_SUCCESS", AuditLog.EVENT_PAYMENT, payment.getPaymentId(),
                "Payment successful via VNPAY", null);

        callback.setProcessingResult("Payment processed successfully");
    }

    private void expireAwaitingPaymentOrder(Order order,
            OrderStatus paymentExpiredStatus,
            PaymentStatus failedPaymentStatus,
            LocalDateTime now) {

        if (order.getOrderStatus() == null
                || !OrderStatus.AWAITING_PAYMENT.equalsIgnoreCase(order.getOrderStatus().getStatusName())) {
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
        if (paymentOpt.isPresent() && isPaidPayment(paymentOpt.get())) {
            log.info("Skipping auto-expire for order {} because payment is already successful", order.getOrderNumber());
            return;
        }
        if (order.getPaidAt() != null) {
            log.info("Skipping auto-expire for order {} because order is already paid", order.getOrderNumber());
            return;
        }

        String timeoutReason = String.format("Payment timeout after %d minutes", vnpayConfig.getTimeoutMinutes());

        order.setOrderStatus(paymentExpiredStatus);
        order.setCancelledAt(now);
        order.setCancellationReason(timeoutReason);
        orderRepository.save(order);

        paymentOpt.ifPresent(payment -> {
            if (!isPaidPayment(payment)) {
                payment.setPaymentStatus(failedPaymentStatus);
                payment.setFailedAt(now);
                payment.setErrorMessage(timeoutReason);
                paymentRepository.save(payment);
            }
        });

        credentialService.releaseOrderCredentials(order);
        notifyPaymentExpired(order);
        createAuditLog(null, "PAYMENT_EXPIRED", AuditLog.EVENT_PAYMENT,
                paymentOpt.map(Payment::getPaymentId).orElse(null),
                timeoutReason, null);

        log.info("Auto-expired awaiting-payment order {} after timeout", order.getOrderNumber());
    }

    /**
     * Handles payment failure.
     */
    @Transactional
    protected void handlePaymentFailure(Payment payment, PaymentCallback callback,
            String responseCode, String transactionStatus) {
        
        log.info("Processing failed payment for order: {}, responseCode: {}, status: {}",
                payment.getOrder().getOrderId(), responseCode, transactionStatus);
        
        // 1. Update payment status
        PaymentStatus failedStatus = paymentStatusRepository.findByStatusName(PaymentStatus.FAILED)
                .orElseThrow(() -> new IllegalStateException("FAILED status not found"));
        payment.setPaymentStatus(failedStatus);
        payment.setFailedAt(LocalDateTime.now());
        payment.setErrorMessage(String.format("ResponseCode: %s, TransactionStatus: %s",
                responseCode, transactionStatus));
        paymentRepository.save(payment);
        
        // 2. Update order status
        Order order = payment.getOrder();
        OrderStatus failedOrderStatus = orderStatusRepository.findByStatusName(OrderStatus.PAYMENT_FAILED)
                .orElseThrow(() -> new IllegalStateException("PAYMENT_FAILED order status not found"));
        order.setOrderStatus(failedOrderStatus);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);

        // 3. Release held credentials for the failed payment
        credentialService.releaseOrderCredentials(order);

        // 4. Create audit log
        createAuditLog(null, "PAYMENT_FAILED", AuditLog.EVENT_PAYMENT, payment.getPaymentId(),
                String.format("Payment failed. ResponseCode: %s", responseCode), null);
        
        callback.setProcessingResult(String.format("Payment failed: %s", responseCode));
    }

    private boolean isPaidPayment(Payment payment) {
        return payment.getPaidAt() != null
                || (payment.getPaymentStatus() != null
                && PaymentStatus.PAID.equalsIgnoreCase(payment.getPaymentStatus().getStatusName()));
    }

    /**
     * Handles payment mismatch (requires manual review).
     */
    @Transactional
    protected void handlePaymentMismatch(Payment payment, PaymentCallback callback, String reason) {
        log.warn("Payment mismatch for order: {}, reason: {}", payment.getOrder().getOrderId(), reason);
        
        // 1. Update payment status to callback mismatch
        PaymentStatus mismatchStatus = paymentStatusRepository.findByStatusName(PaymentStatus.CALLBACK_MISMATCH)
                .orElseThrow(() -> new IllegalStateException("CALLBACK_MISMATCH status not found"));
        payment.setPaymentStatus(mismatchStatus);
        payment.setErrorMessage(reason);
        paymentRepository.save(payment);
        
        // 2. Create audit log
        createAuditLog(null, "PAYMENT_MISMATCH", AuditLog.EVENT_PAYMENT, payment.getPaymentId(),
                reason, null);
        
        callback.setProcessingResult(String.format("Payment mismatch: %s", reason));
    }

    /**
     * Creates an escrow record for a paid order.
     */
    @Transactional
    protected void createEscrowForOrder(Order order) {
        // Check if escrow already exists
        if (escrowRepository.findByOrder(order).isPresent()) {
            log.info("Escrow already exists for order: {}", order.getOrderId());
            return;
        }
        
        // Calculate platform fee (example: 5%)
        BigDecimal grossAmount = order.getTotalAmount();
        BigDecimal platformFeeRate = new BigDecimal("0.05");
        BigDecimal platformFee = grossAmount.multiply(platformFeeRate);
        BigDecimal netAmount = grossAmount.subtract(platformFee);
        
        // Get holding status
        EscrowStatus holdingStatus = escrowStatusRepository.findByStatusName(EscrowStatus.HOLDING)
                .orElseThrow(() -> new IllegalStateException("HOLDING status not found"));
        
        // Create escrow
        Escrow escrow = new Escrow();
        escrow.setOrder(order);
        escrow.setAmount(grossAmount);
        escrow.setPlatformFee(platformFee);
        escrow.setSellerAmount(netAmount);
        escrow.setEscrowStatus(holdingStatus);

        // Set release time (24 hours from now for buyer verification)
        LocalDateTime releaseTime = LocalDateTime.now().plusHours(24);
        escrow.setAutoReleaseDeadline(releaseTime);

        escrowRepository.save(escrow);

        // Create escrow transaction for HOLD
        EscrowTransaction transaction = new EscrowTransaction();
        transaction.setEscrow(escrow);
        transaction.setTransactionType("HOLD");
        transaction.setAmount(grossAmount);
        transaction.setBalanceAfter(grossAmount);
        transaction.setDescription("Initial escrow hold after successful payment");
        transaction.setReferenceType("ORDER");
        transaction.setReferenceId(order.getOrderId());

        escrowTransactionRepository.save(transaction);
        
        log.info("Created escrow for order: {}, amount: {}, release scheduled: {}", 
                order.getOrderId(), grossAmount, releaseTime);
    }

    /**
     * Creates an audit log entry.
     */
    private void createAuditLog(Integer userId, String action, String entityType, 
            Long entityId, String description, String metadata) {

        AuditLog auditLog = AuditLog.builder()
                .eventType(entityType)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .metadata(metadata)
                .performerRole(AuditLog.ROLE_SYSTEM)
                .success(true)
                .build();

        auditLogRepository.save(auditLog);
    }

    private void notifyPaymentExpired(Order order) {
        notificationService.createNotification(
                order.getBuyer(),
                Notification.TYPE_PAYMENT,
                NotificationPreference.CATEGORY_PAYMENT,
                "Đơn hàng " + order.getOrderNumber() + " đã hết hạn thanh toán",
                "Phiên thanh toán đã quá thời gian chờ. Tài khoản giữ chỗ đã được hoàn lại kho, vui lòng checkout lại nếu vẫn muốn mua.",
                Notification.PRIORITY_NORMAL,
                "ORDER",
                order.getOrderId(),
                "/buyer/purchases?orderId=" + order.getOrderId()
        );

        User seller = order.getOrderItems().stream()
                .map(OrderItem::getSeller)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (seller != null) {
            notificationService.createNotification(
                    seller,
                    Notification.TYPE_ORDER,
                    NotificationPreference.CATEGORY_ORDER,
                    "Đơn hàng " + order.getOrderNumber() + " đã hết hạn thanh toán",
                    "Buyer chưa hoàn tất thanh toán trong thời gian cho phép. Credential giữ chỗ đã được hoàn lại kho.",
                    Notification.PRIORITY_NORMAL,
                    "ORDER",
                    order.getOrderId(),
                    "/seller/orders"
            );
        }
    }

    /**
     * Result class for payment processing.
     */
    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final Long orderId;
        private final boolean topUp;
        private final Integer pendingPostId;
        private final boolean pending;
        private final boolean sandboxReturnConfirmed;

        private PaymentResult(boolean success, String message, Long orderId, boolean topUp,
                              Integer pendingPostId, boolean pending, boolean sandboxReturnConfirmed) {
            this.success = success;
            this.message = message;
            this.orderId = orderId;
            this.topUp = topUp;
            this.pendingPostId = pendingPostId;
            this.pending = pending;
            this.sandboxReturnConfirmed = sandboxReturnConfirmed;
        }

        public static PaymentResult success(String message, Long orderId) {
            return new PaymentResult(true, message, orderId, false, null, false, false);
        }

        public static PaymentResult failed(String message, Long orderId) {
            return new PaymentResult(false, message, orderId, false, null, false, false);
        }

        public static PaymentResult topUpSuccess(String message) {
            return new PaymentResult(true, message, null, true, null, false, false);
        }

        public static PaymentResult topUpSuccessWithRedirect(String message, Integer pendingPostId) {
            return new PaymentResult(true, message, null, true, pendingPostId, false, false);
        }

        public static PaymentResult topUpSandboxReturnConfirmed(String message) {
            return new PaymentResult(true, message, null, true, null, false, true);
        }

        public static PaymentResult topUpSandboxReturnConfirmedWithRedirect(String message, Integer pendingPostId) {
            return new PaymentResult(true, message, null, true, pendingPostId, false, true);
        }

        public static PaymentResult topUpPending(String message, Integer pendingPostId) {
            return new PaymentResult(false, message, null, true, pendingPostId, true, false);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getOrderId() { return orderId; }
        public boolean isTopUp() { return topUp; }
        public Integer getPendingPostId() { return pendingPostId; }
        public boolean isPending() { return pending; }
        public boolean isSandboxReturnConfirmed() { return sandboxReturnConfirmed; }
    }
}
