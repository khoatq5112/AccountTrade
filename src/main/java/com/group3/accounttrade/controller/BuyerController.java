package com.group3.accounttrade.controller;

import com.group3.accounttrade.dto.DisputeDetailDTO;
import com.group3.accounttrade.dto.DisputeMessageDTO;
import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.Order;
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

@Slf4j
@Controller
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final PostRepository postRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CartService cartService;
    private final BuyerOrderService buyerOrderService;
    private final OrderCheckoutService orderCheckoutService;
    private final DisputeService disputeService;
    private final CloudinaryService cloudinaryService;

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
    public String buyerWallet(Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html";
        }
        model.addAttribute("currentUser", user);
        Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(null);
        model.addAttribute("walletBalance", wallet != null ? wallet.getBalance() : BigDecimal.ZERO);
        return "buyer_wallet";
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam Integer postId, Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/checkout?postId=" + postId;
        }
        if (isSellerAccount(user)) {
            return "redirect:/marketplace/" + postId + "?error=seller_restricted";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return "redirect:/marketplace?error=not_found";
        }

        if (post.getSeller() != null && post.getSeller().getUserId().equals(user.getUserId())) {
            return "redirect:/marketplace/" + postId + "?error=own_post";
        }

        if (!post.isInStock()) {
            return "redirect:/marketplace/" + postId + "?error=unavailable";
        }

        model.addAttribute("post", post);
        model.addAttribute("user", user);
        model.addAttribute("isAuthenticated", true);
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
    public String processCheckout(@RequestParam Integer postId,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login.html?redirect=/buyer/checkout?postId=" + postId;
        }
        if (isSellerAccount(user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản seller không thể mua sản phẩm.");
            return "redirect:/marketplace/" + postId;
        }

        try {
            OrderCheckoutService.CheckoutSession checkoutSession = orderCheckoutService
                    .initiateCheckout(user, postId, request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đơn hàng đã được tạo. Hoàn tất thanh toán để TrustBridge chuyển giao credential theo quy trình escrow.");
            return "redirect:" + checkoutSession.paymentUrl();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/marketplace/" + postId;
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            if ("VNPAY is not properly configured".equals(message)) {
                message = "VNPAY sandbox is not configured correctly. Check the VNPAY properties or environment variables before retrying checkout.";
            }
            redirectAttributes.addFlashAttribute("errorMessage", message);
            return "redirect:/marketplace/" + postId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể khởi tạo phiên thanh toán cho sản phẩm này.");
            return "redirect:/marketplace/" + postId;
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

        List<Order> workflowOrders;
        workflowOrders = orderRepository.findByBuyer(user).stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
        
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
            log.info("[DISPUTE] User {} opening dispute for order {}", user.getUsername(), orderId);
            
            // Upload images to Cloudinary and collect URLs
            List<String> imageUrls = new ArrayList<>();
            if (evidenceImages != null && evidenceImages.length > 0) {
                log.info("[DISPUTE] Processing {} evidence images", evidenceImages.length);
                for (MultipartFile file : evidenceImages) {
                    if (file != null && !file.isEmpty()) {
                        try {
                            String imageUrl = cloudinaryService.uploadImage(file);
                            if (imageUrl != null) {
                                imageUrls.add(imageUrl);
                                log.info("[DISPUTE] Uploaded image: {}", imageUrl);
                            }
                        } catch (Exception e) {
                            log.warn("[DISPUTE] Failed to upload dispute evidence image: {}", e.getMessage());
                        }
                    }
                }
            }

            // Open dispute with image evidence
            log.info("[DISPUTE] Calling disputeService.openDispute with orderId={}, buyerId={}, reason={}, description={}, imageCount={}",
                    orderId, user.getUserId(), reason, description, imageUrls.size());
            
            Dispute dispute = disputeService.openDispute(orderId, user.getUserId(), reason, description, imageUrls);
            
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
}
