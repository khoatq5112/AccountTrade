package com.group3.accounttrade.controller;

import com.group3.accounttrade.dto.DisputeDetailDTO;
import com.group3.accounttrade.dto.DisputeMessageDTO;
import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.entity.Wallet;
import com.group3.accounttrade.repository.OrderRepository;
import com.group3.accounttrade.repository.OrderStatusRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
import com.group3.accounttrade.service.BuyerOrderService;
import com.group3.accounttrade.service.CartService;
import com.group3.accounttrade.service.CloudinaryService;
import com.group3.accounttrade.service.DisputeService;
import com.group3.accounttrade.service.OrderCheckoutService;
import com.group3.accounttrade.service.WalletService;
import com.group3.accounttrade.util.IdEncoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {
    private static final int MAX_DISPUTE_REASON_LENGTH = 100;
    private static final int MAX_DISPUTE_DESCRIPTION_LENGTH = 2000;
    private static final Set<String> ALLOWED_DISPUTE_REASONS = Set.of(
            DisputeService.REASON_INVALID_CREDENTIAL,
            DisputeService.REASON_CREDENTIAL_CHANGED,
            DisputeService.REASON_NOT_AS_DESCRIBED,
            DisputeService.REASON_NO_CREDENTIAL_RECEIVED,
            DisputeService.REASON_OTHER
    );

    private final PostRepository postRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CartService cartService;
    private final BuyerOrderService buyerOrderService;
    private final OrderCheckoutService orderCheckoutService;
    private final DisputeService disputeService;
    private final CloudinaryService cloudinaryService;
    private final WalletService walletService;
    private final IdEncoder idEncoder;

    @GetMapping("/dashboard")
    public String viewBuyerDashboard(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html";
        }

        // Add user info
        model.addAttribute("currentUser", user);

        // Get wallet balance
        Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(null);
        BigDecimal walletBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
        model.addAttribute("walletBalance", walletBalance);

        BuyerOrderService.BuyerDashboardSummary summary = buyerOrderService.getDashboardSummary(user);

        model.addAttribute("escrowAmount", summary.escrowAmount());
        model.addAttribute("escrowCount", summary.escrowCount());
        model.addAttribute("totalSpent", summary.totalSpent());
        model.addAttribute("completedCount", summary.completedCount());
        model.addAttribute("pendingOrders", summary.pendingOrders());

        return "buyer_dashboard";
    }

    @GetMapping("/settings")
    public String buyerSettings(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html";
        }
        model.addAttribute("currentUser", user);
        Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(null);
        model.addAttribute("walletBalance", wallet != null ? wallet.getBalance() : BigDecimal.ZERO);
        return "buyer_settings";
    }

    @GetMapping("/wallet")
    public String buyerWallet(Model model,
                               @RequestParam(required = false) Boolean topupSuccess,
                               @RequestParam(required = false) Boolean topupSandboxReturnConfirmed,
                               @RequestParam(required = false) Boolean topupPending,
                               @RequestParam(required = false) Boolean topupFailed) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html";
        }
        model.addAttribute("currentUser", user);
        BigDecimal walletBalance = walletService.getBalance(user);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("transactionHistory", walletService.getTransactionHistory(user));
        model.addAttribute("topUpHistory", walletService.getTopUpHistory(user));
        addTopUpOptions(model);
        if (Boolean.TRUE.equals(topupSuccess)) {
            model.addAttribute("successMessage", "Nạp tiền vào ví thành công!");
        }
        if (Boolean.TRUE.equals(topupSandboxReturnConfirmed)) {
            model.addAttribute("infoMessage", "Sandbox VNPAY đã xác nhận top-up ngay trên callback RETURN. Trong production, ví sẽ chờ IPN để hoàn tất.");
        }
        if (Boolean.TRUE.equals(topupPending)) {
            model.addAttribute("infoMessage", "VNPAY đang đồng bộ giao dịch nạp tiền. Số dư ví sẽ cập nhật ngay khi IPN được xác nhận.");
        }
        if (Boolean.TRUE.equals(topupFailed)) {
            model.addAttribute("errorMessage", "Nạp tiền thất bại. Vui lòng thử lại.");
        }
        return "buyer_wallet";
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam String postId,
                           @RequestParam(required = false) Boolean topupSuccess,
                           @RequestParam(required = false) Boolean topupSandboxReturnConfirmed,
                           @RequestParam(required = false) Boolean topupPending,
                           Model model) {
        Integer decodedPostId;
        try {
            decodedPostId = idEncoder.decodePostId(postId);
        } catch (IllegalArgumentException e) {
            return "redirect:/marketplace?error=not_found";
        }

        String encodedPostId = idEncoder.encodePostId(decodedPostId);
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/checkout?postId=" + encodedPostId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace/" + encodedPostId + "?error=seller_restricted";
        }

        try {
            OrderCheckoutService.WalletCheckoutPreview preview = orderCheckoutService.previewWalletCheckout(user, decodedPostId);
            model.addAttribute("post", preview.post());
            model.addAttribute("walletBalance", preview.walletBalance());
            model.addAttribute("walletDeficit", preview.deficitAmount());
            model.addAttribute("suggestedTopUpAmount", preview.suggestedTopUpAmount());
            model.addAttribute("hasSufficientBalance", preview.hasSufficientBalance());
            addTopUpOptions(model);
        } catch (IllegalArgumentException e) {
            if ("You cannot purchase your own post".equals(e.getMessage())) {
                return "redirect:/marketplace/" + encodedPostId + "?error=own_post";
            }
            return "redirect:/marketplace?error=not_found";
        } catch (IllegalStateException e) {
            return "redirect:/marketplace/" + encodedPostId + "?error=unavailable";
        }

        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
        if (Boolean.TRUE.equals(topupSuccess)) {
            model.addAttribute("successMessage", "Nạp tiền thành công. Bạn có thể hoàn tất thanh toán bằng ví ngay bây giờ.");
        }
        if (Boolean.TRUE.equals(topupSandboxReturnConfirmed)) {
            model.addAttribute("infoMessage", "Sandbox VNPAY đã xác nhận top-up ngay trên callback RETURN. Bạn có thể tiếp tục checkout bằng số dư mới.");
        }
        if (Boolean.TRUE.equals(topupPending)) {
            model.addAttribute("infoMessage", "Giao dịch nạp tiền đang chờ VNPAY xác nhận. Trang này sẽ dùng số dư mới ngay khi IPN hoàn tất.");
        }
        return "checkout";
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/cart";
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        List<CartService.CartItemDto> cartItems = cartService.getUserCartItems(user);
        BigDecimal subtotal = cartItems.stream()
                .map(CartService.CartItemDto::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("cartCount", cartItems.size());
        model.addAttribute("isAuthenticated", true);
        return "cart";
    }

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String postId,
                                  @RequestParam(defaultValue = "VNPAY") String paymentMethod,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        Integer decodedPostId;
        try {
            decodedPostId = idEncoder.decodePostId(postId);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm không hợp lệ.");
            return "redirect:/marketplace?error=not_found";
        }

        String encodedPostId = idEncoder.encodePostId(decodedPostId);
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/checkout?postId=" + encodedPostId;
        }
        if (isSellerAccount(user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản seller không thể mua sản phẩm.");
            return "redirect:/marketplace/" + encodedPostId;
        }

        PaymentMethod resolvedPaymentMethod;
        try {
            resolvedPaymentMethod = PaymentMethod.from(paymentMethod);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/marketplace/" + encodedPostId;
        }

        if (resolvedPaymentMethod == PaymentMethod.WALLET) {
            try {
                Order order = orderCheckoutService.initiateWalletCheckout(user, decodedPostId);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán bằng ví thành công! Credential đã được giao cho đơn hàng của bạn.");
                return "redirect:/buyer/purchases?orderId=" + order.getOrderId();
            } catch (WalletService.InsufficientBalanceException e) {
                log.warn("[CHECKOUT] Wallet balance insufficient for user {} on post {}: {}",
                        user.getUsername(), decodedPostId, e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/buyer/checkout?postId=" + encodedPostId;
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.error("[CHECKOUT] Wallet checkout failed validation for user {} (id={}) on post {}",
                        user.getUsername(), user.getUserId(), decodedPostId, e);
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/marketplace/" + encodedPostId;
            } catch (Exception e) {
                log.error("[CHECKOUT] Unexpected wallet checkout failure for user {} (id={}) on post {}",
                        user.getUsername(), user.getUserId(), decodedPostId, e);
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán bằng ví thất bại. Vui lòng thử lại.");
                return "redirect:/buyer/checkout?postId=" + encodedPostId;
            }
        }

        try {
            OrderCheckoutService.CheckoutSession checkoutSession = orderCheckoutService
                    .initiateCheckout(user, decodedPostId, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đơn hàng đã được tạo. Hoàn tất thanh toán để TrustBridge chuyển giao credential theo quy trình escrow.");
            return "redirect:" + checkoutSession.paymentUrl();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/marketplace/" + encodedPostId;
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            if ("VNPAY is not properly configured".equals(message)) {
                message = "VNPAY sandbox is not configured correctly. Check the VNPAY properties or environment variables before retrying checkout.";
            }
            redirectAttributes.addFlashAttribute("errorMessage", message);
            return "redirect:/marketplace/" + encodedPostId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể khởi tạo phiên thanh toán cho sản phẩm này.");
            return "redirect:/marketplace/" + encodedPostId;
        }
    }

    @PostMapping("/wallet/topup")
    public String initiateTopUp(@RequestParam BigDecimal amount,
                                 @RequestParam(required = false) BigDecimal requiredAmount,
                                 @RequestParam(required = false) String pendingPostId,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html";
        }
        Integer decodedPendingPostId = null;
        if (pendingPostId != null && !pendingPostId.isBlank()) {
            try {
                decodedPendingPostId = idEncoder.decodePostId(pendingPostId);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm chờ thanh toán không hợp lệ.");
                return "redirect:/buyer/wallet";
            }
        }
        try {
            String paymentUrl = walletService.initiateTopUp(user, amount, requiredAmount, decodedPendingPostId, request);
            return "redirect:" + paymentUrl;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return resolveTopUpRedirect(pendingPostId);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return resolveTopUpRedirect(pendingPostId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể khởi tạo nạp tiền. Vui lòng thử lại.");
            return resolveTopUpRedirect(pendingPostId);
        }
    }

    @GetMapping("/purchases")
    public String viewPurchases(@RequestParam(required = false) Long orderId,
                                @RequestParam(required = false) Long revealCredentialsOrderId,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        log.info("[DEBUG] viewPurchases called - orderId: {}, revealCredentialsOrderId: {}", orderId, revealCredentialsOrderId);
        
        User user = getCurrentUser();
        if (user == null) {
            log.warn("[DEBUG] User is null, redirecting to login");
            return "redirect:/login.html?redirect=/buyer/purchases";
        }
        if (isSellerAccount(user)) {
            log.warn("[DEBUG] User is seller, redirecting to marketplace");
            return "redirect:/marketplace?error=seller_restricted";
        }

        log.info("[DEBUG] User authenticated: {}", user.getUsername());

        // Add user info for dashboard layout
        model.addAttribute("currentUser", user);

        // Get wallet balance for nav display
        Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(null);
        BigDecimal walletBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
        model.addAttribute("walletBalance", walletBalance);

        List<Order> allOrders = orderRepository.findByBuyer(user).stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
        List<Order> workflowOrders = allOrders.stream()
                .filter(order -> shouldDisplayInPurchases(order, orderId))
                .collect(Collectors.toList());
        
        model.addAttribute("workflowOrders", workflowOrders);
        model.addAttribute("highlightOrderId", orderId);
        model.addAttribute("revealedCredentialsOrderId", revealCredentialsOrderId);
        if (revealCredentialsOrderId != null) {
            try {
                log.info("[DEBUG] Revealing credentials for orderId: {}", revealCredentialsOrderId);
                model.addAttribute("revealedCredentials",
                        buyerOrderService.revealCredentials(user, revealCredentialsOrderId, request.getRemoteAddr()));
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.error("[DEBUG] Error revealing credentials: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/buyer/purchases";
            }
        }
        model.addAttribute("isAuthenticated", true);
        log.info("[DEBUG] Returning buyer_purchases template");
        return "buyer_purchases";
    }

    @PostMapping("/orders/{orderId}/confirm")
    public String confirmOrderReceipt(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/purchases";
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            buyerOrderService.confirmReceipt(user, orderId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bạn đã xác nhận nhận hàng thành công. Đơn hàng đã hoàn tất và tiền đã được chuyển cho seller.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/buyer/purchases?orderId=" + orderId;
    }

    @PostMapping("/orders/{orderId}/disputes")
    public String openOrderDispute(@PathVariable Long orderId,
                                   @RequestParam String reason,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) MultipartFile[] evidenceImages,
                                   RedirectAttributes redirectAttributes) {
        log.info("[DISPUTE] Opening dispute for order: {}, reason: {}", orderId, reason);
        
        User user = getCurrentUser();
        if (user == null) {
            log.warn("[DISPUTE] User is null, redirecting to login");
            return "redirect:/login.html?redirect=/buyer/purchases";
        }
        if (isSellerAccount(user)) {
            log.warn("[DISPUTE] User is seller, redirecting to marketplace");
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            String normalizedReason = normalizeDisputeReason(reason);
            String normalizedDescription = normalizeDisputeDescription(description);
            log.info("[DISPUTE] User {} opening dispute for order {}", user.getUsername(), orderId);
            
            // Upload images to Cloudinary and collect URLs
            List<String> imageUrls = new ArrayList<>();
            int selectedEvidenceCount = 0;
            if (evidenceImages != null && evidenceImages.length > 0) {
                log.info("[DISPUTE] Processing {} evidence images", evidenceImages.length);
                for (MultipartFile file : evidenceImages) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    selectedEvidenceCount++;
                    try {
                        String imageUrl = cloudinaryService.uploadImage(file);
                        if (imageUrl == null || imageUrl.isBlank()) {
                            throw new IllegalStateException("Cloudinary did not return a URL for dispute evidence");
                        }
                        imageUrls.add(imageUrl);
                        log.info("[DISPUTE] Uploaded image: {}", imageUrl);
                    } catch (Exception e) {
                        log.warn("[DISPUTE] Failed to upload dispute evidence image: {}", e.getMessage(), e);
                        throw new IllegalStateException("Không thể tải ảnh bằng chứng lên. Vui lòng thử lại.");
                    }
                }
            }
            if (selectedEvidenceCount > 0 && imageUrls.size() != selectedEvidenceCount) {
                throw new IllegalStateException("Không thể tải đầy đủ ảnh bằng chứng lên. Vui lòng thử lại.");
            }

            // Open dispute with image evidence
            log.info("[DISPUTE] Calling disputeService.openDispute with orderId={}, buyerId={}, reason={}, description={}, imageCount={}",
                    orderId, user.getUserId(), normalizedReason, normalizedDescription, imageUrls.size());
            
            Dispute dispute = disputeService.openDispute(orderId, user.getUserId(), normalizedReason, normalizedDescription, imageUrls);
            
            log.info("[DISPUTE] Dispute created successfully with ID: {}", dispute.getDisputeId());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Khiếu nại đã được tạo. Escrow đã bị đóng băng và TrustBridge sẽ chỉ can thiệp ở bước xử lý tranh chấp.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[DISPUTE] Failed to create dispute: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("[DISPUTE] Unexpected error creating dispute: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo khiếu nại: " + e.getMessage());
        }

        return "redirect:/buyer/purchases?orderId=" + orderId;
    }

    // ==================== DISPUTE MANAGEMENT ENDPOINTS ====================

    /**
     * List all disputes for the current buyer.
     */
    @GetMapping("/disputes")
    public String listBuyerDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes";
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        // Add user info
        model.addAttribute("currentUser", user);
        
        // Get wallet balance
        Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(null);
        BigDecimal walletBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
        model.addAttribute("walletBalance", walletBalance);

        // Get disputes with pagination
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.group3.accounttrade.entity.Dispute> disputesPage = 
                disputeService.getDisputesByBuyerPaginated(user.getUserId(), status, pageable);

        model.addAttribute("disputesPage", disputesPage);
        model.addAttribute("disputes", disputesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", disputesPage.getTotalPages());
        model.addAttribute("totalItems", disputesPage.getTotalElements());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("isAuthenticated", true);

        return "buyer_disputes";
    }

    /**
     * View dispute detail.
     */
    @GetMapping("/disputes/{disputeId}")
    public String viewDisputeDetail(@PathVariable Long disputeId,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes";
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        // Get dispute detail
        com.group3.accounttrade.dto.DisputeDetailDTO dispute = disputeService.getDisputeDetailDTO(disputeId);
        if (dispute == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khiếu nại.");
            return "redirect:/buyer/disputes";
        }

        // Verify user is the buyer (openedBy)
        if (!dispute.buyer().userId().equals(user.getUserId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem khiếu nại này.");
            return "redirect:/buyer/disputes";
        }

        // Add user info
        model.addAttribute("currentUser", user);
        
        // Get wallet balance
        Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(null);
        BigDecimal walletBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
        model.addAttribute("walletBalance", walletBalance);

        model.addAttribute("dispute", dispute);
        model.addAttribute("isAuthenticated", true);

        return "buyer_dispute_detail";
    }

    /**
     * Add message to dispute.
     */
    @PostMapping("/disputes/{disputeId}/messages")
    public String addDisputeMessage(@PathVariable Long disputeId,
                                      @RequestParam String message,
                                      @RequestParam(required = false) String attachmentUrl,
                                      RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes/" + disputeId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            disputeService.addMessage(disputeId, user.getUserId(), message, attachmentUrl);
            redirectAttributes.addFlashAttribute("successMessage", "Tin nhắn đã được gửi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/buyer/disputes/" + disputeId;
    }

    /**
     * Cancel dispute (buyer only).
     */
    @PostMapping("/disputes/{disputeId}/cancel")
    public String cancelDispute(@PathVariable Long disputeId,
                                 @RequestParam String reason,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes/" + disputeId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            disputeService.cancelDispute(disputeId, user.getUserId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Khiếu nại đã được hủy.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/buyer/disputes";
    }

    @PostMapping("/disputes/{disputeId}/accept-refund")
    public String acceptSellerRefund(@PathVariable Long disputeId, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes/" + disputeId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            disputeService.acceptSellerRefund(disputeId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã chấp nhận đề xuất hoàn tiền của seller.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/buyer/disputes/" + disputeId;
    }

    @PostMapping("/disputes/{disputeId}/accept-replacement")
    public String acceptSellerReplacement(@PathVariable Long disputeId, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes/" + disputeId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            disputeService.acceptSellerReplacement(disputeId, user.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã chấp nhận phương án thay thế. Hãy kiểm tra credential mới.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/buyer/disputes/" + disputeId;
    }

    @PostMapping("/disputes/{disputeId}/replacement-failed")
    public String reportReplacementFailure(@PathVariable Long disputeId,
                                           @RequestParam(required = false) String reason,
                                           RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes/" + disputeId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            disputeService.reportReplacementFailure(disputeId, user.getUserId(), reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển dispute sang admin vì credential thay thế vẫn không hoạt động.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/buyer/disputes/" + disputeId;
    }

    @PostMapping("/disputes/{disputeId}/escalate")
    public String escalateDispute(@PathVariable Long disputeId,
                                  @RequestParam(required = false) String reason,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/disputes/" + disputeId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace?error=seller_restricted";
        }

        try {
            disputeService.escalateDispute(disputeId, user.getUserId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Khiếu nại đã được chuyển sang bước admin xem xét.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/buyer/disputes/" + disputeId;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean isSellerAccount(User user) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRoleName() != null
                && "Seller".equalsIgnoreCase(user.getRole().getRoleName());
    }

    private String resolveTopUpRedirect(String pendingPostId) {
        if (pendingPostId != null) {
            return "redirect:/buyer/checkout?postId=" + pendingPostId;
        }
        return "redirect:/buyer/wallet";
    }

    private boolean shouldDisplayInPurchases(Order order, Long highlightedOrderId) {
        if (order == null) {
            return false;
        }
        if (highlightedOrderId != null && highlightedOrderId.equals(order.getOrderId())) {
            return true;
        }
        if (order.getOrderStatus() == null) {
            return true;
        }
        String statusName = order.getOrderStatus().getStatusName();
        return !OrderStatus.PAYMENT_EXPIRED.equalsIgnoreCase(statusName)
                && !OrderStatus.PAYMENT_FAILED.equalsIgnoreCase(statusName);
    }

    private void addTopUpOptions(Model model) {
        model.addAttribute("minimumTopUpAmount", walletService.getMinimumTopUpAmount());
        model.addAttribute("maximumTopUpAmount", walletService.getMaximumTopUpAmount());
        model.addAttribute("topUpPresetAmounts", walletService.getPresetTopUpAmounts());
    }

    private String normalizeDisputeReason(String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn lý do khiếu nại.");
        }
        if (normalized.length() > MAX_DISPUTE_REASON_LENGTH) {
            throw new IllegalArgumentException("Lý do khiếu nại quá dài.");
        }
        if (!ALLOWED_DISPUTE_REASONS.contains(normalized)) {
            throw new IllegalArgumentException("Lý do khiếu nại không hợp lệ.");
        }
        return normalized;
    }

    private String normalizeDisputeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > MAX_DISPUTE_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Mô tả khiếu nại không được vượt quá 2000 ký tự.");
        }
        return normalized.isEmpty() ? null : normalized;
    }
}
