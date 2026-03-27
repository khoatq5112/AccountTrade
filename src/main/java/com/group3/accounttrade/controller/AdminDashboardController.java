package com.group3.accounttrade.controller;

import com.group3.accounttrade.dto.AdminDashboardStats;
import com.group3.accounttrade.dto.ChartDataPoint;
import com.group3.accounttrade.dto.CommissionConfigDTO;
import com.group3.accounttrade.dto.CommissionEarningSummaryDTO;
import com.group3.accounttrade.dto.DisputeDTO;
import com.group3.accounttrade.dto.DisputeDetailDTO;
import com.group3.accounttrade.dto.PendingPostDTO;
import com.group3.accounttrade.dto.PlatformEarningDTO;
import com.group3.accounttrade.dto.TransactionDTO;
import com.group3.accounttrade.dto.TransactionStatsDTO;
import com.group3.accounttrade.dto.UserDTO;
import com.group3.accounttrade.entity.CommissionConfig;
import com.group3.accounttrade.entity.PlatformEarning;
import com.group3.accounttrade.service.AdminDashboardService;
import com.group3.accounttrade.service.AdminTransactionService;
import com.group3.accounttrade.service.CommissionService;
import com.group3.accounttrade.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for admin dashboard API endpoints.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final DisputeService disputeService;
    private final AdminTransactionService adminTransactionService;
    private final CommissionService commissionService;

    /**
     * Gets aggregated dashboard statistics.
     *
     * @return AdminDashboardStats containing all platform metrics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardStats> getDashboardStats() {
        AdminDashboardStats stats = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets posts pending admin approval.
     *
     * @param limit maximum number of posts to return (default 10)
     * @return list of pending posts
     */
    @GetMapping("/pending-posts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PendingPostDTO>> getPendingPosts(
            @RequestParam(defaultValue = "10") int limit) {
        List<PendingPostDTO> posts = adminDashboardService.getPendingApprovals(limit);
        return ResponseEntity.ok(posts);
    }

    /**
     * Gets disputes requiring admin attention.
     *
     * @param limit maximum number of disputes to return (default 10)
     * @return list of pending disputes
     */
    @GetMapping("/disputes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DisputeDTO>> getPendingDisputes(
            @RequestParam(defaultValue = "10") int limit) {
        List<DisputeDTO> disputes = adminDashboardService.getPendingDisputes(limit);
        return ResponseEntity.ok(disputes);
    }

    /**
     * Gets transaction volume chart data.
     *
     * @param days number of days to include (default 7)
     * @return list of chart data points
     */
    @GetMapping("/chart-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ChartDataPoint>> getChartData(
            @RequestParam(defaultValue = "7") int days) {
        List<ChartDataPoint> chartData = adminDashboardService.getTransactionVolumeChartData(days);
        return ResponseEntity.ok(chartData);
    }

    /**
     * Approves a pending post.
     *
     * @param postId the post ID to approve
     * @return success response
     */
    @PostMapping("/posts/{postId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approvePost(@PathVariable Integer postId) {
        boolean success = adminDashboardService.approvePost(postId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", "Post approved successfully");
        response.put("postId", postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Rejects a pending post.
     *
     * @param postId the post ID to reject
     * @return success response
     */
    @PostMapping("/posts/{postId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> rejectPost(@PathVariable Integer postId) {
        boolean success = adminDashboardService.rejectPost(postId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", "Post rejected successfully");
        response.put("postId", postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets recent users for user management section.
     *
     * @param limit maximum number of users to return (default 10)
     * @return list of recent users
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsers(
            @RequestParam(defaultValue = "10") int limit) {
        List<UserDTO> users = adminDashboardService.getRecentUsers(limit);
        return ResponseEntity.ok(users);
    }

    // ==================== Dispute Management Endpoints ====================

    /**
     * Gets paginated disputes with optional status filter.
     *
     * @param status Optional status filter
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of disputes
     */
    @GetMapping("/disputes/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<com.group3.accounttrade.dto.DisputeDTO> disputesPage = disputeService.getAllDisputesPaginated(status, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", disputesPage.getContent());
        response.put("totalElements", disputesPage.getTotalElements());
        response.put("totalPages", disputesPage.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets detailed dispute information.
     *
     * @param disputeId The dispute ID
     * @return DisputeDetailDTO
     */
    @GetMapping("/disputes/{disputeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeDetailDTO> getDisputeDetail(@PathVariable Long disputeId) {
        DisputeDetailDTO dispute = disputeService.getDisputeDetailDTO(disputeId);
        if (dispute == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dispute);
    }

    /**
     * Assigns an admin to a dispute.
     *
     * @param disputeId The dispute ID
     * @param authentication The current authentication
     * @return Success response
     */
    @PostMapping("/disputes/{disputeId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignDispute(
            @PathVariable Long disputeId,
            Authentication authentication) {
        
        com.group3.accounttrade.entity.User admin = adminDashboardService.getUserByUsername(authentication.getName());
        if (admin == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin not found"));
        }
        
        disputeService.assignAdmin(disputeId, admin.getUserId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Dispute assigned successfully");
        response.put("disputeId", disputeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Starts admin review of a dispute.
     *
     * @param disputeId The dispute ID
     * @param authentication The current authentication
     * @return Success response
     */
    @PostMapping("/disputes/{disputeId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> startDisputeReview(
            @PathVariable Long disputeId,
            Authentication authentication) {
        
        com.group3.accounttrade.entity.User admin = adminDashboardService.getUserByUsername(authentication.getName());
        if (admin == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin not found"));
        }
        
        try {
            disputeService.startReview(disputeId, admin.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dispute review started");
            response.put("disputeId", disputeId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Resolves a dispute in buyer's favor.
     *
     * @param disputeId The dispute ID
     * @param resolution Resolution notes
     * @param refundAmount Optional partial refund amount
     * @param authentication The current authentication
     * @return Success response
     */
    @PostMapping("/disputes/{disputeId}/resolve/buyer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resolveInBuyerFavor(
            @PathVariable Long disputeId,
            @RequestParam String resolution,
            @RequestParam(required = false) BigDecimal refundAmount,
            Authentication authentication) {
        
        log.info("[DEBUG] resolveInBuyerFavor called - disputeId: {}, resolution: {}, refundAmount: {}",
                disputeId, resolution, refundAmount);
        
        com.group3.accounttrade.entity.User admin = adminDashboardService.getUserByUsername(authentication.getName());
        if (admin == null) {
            log.error("[DEBUG] Admin not found for authentication: {}", authentication.getName());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin not found"));
        }
        
        try {
            log.info("[DEBUG] Calling disputeService.resolveInBuyerFavor with adminId: {}", admin.getUserId());
            disputeService.resolveInBuyerFavor(disputeId, admin.getUserId(), resolution, refundAmount);
            log.info("[DEBUG] resolveInBuyerFavor completed successfully");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dispute resolved in buyer's favor");
            response.put("disputeId", disputeId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("[DEBUG] IllegalArgumentException in resolveInBuyerFavor: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("[DEBUG] IllegalStateException in resolveInBuyerFavor: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[DEBUG] UNEXPECTED EXCEPTION in resolveInBuyerFavor: {} - {}", e.getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    /**
     * Resolves a dispute in seller's favor.
     *
     * @param disputeId The dispute ID
     * @param resolution Resolution notes
     * @param authentication The current authentication
     * @return Success response
     */
    @PostMapping("/disputes/{disputeId}/resolve/seller")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resolveInSellerFavor(
            @PathVariable Long disputeId,
            @RequestParam String resolution,
            Authentication authentication) {
        
        com.group3.accounttrade.entity.User admin = adminDashboardService.getUserByUsername(authentication.getName());
        if (admin == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin not found"));
        }
        
        try {
            disputeService.resolveInSellerFavor(disputeId, admin.getUserId(), resolution);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dispute resolved in seller's favor");
            response.put("disputeId", disputeId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Adds admin notes to a dispute.
     *
     * @param disputeId The dispute ID
     * @param notes The notes to add
     * @param authentication The current authentication
     * @return Success response
     */
    @PostMapping("/disputes/{disputeId}/notes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> addDisputeNotes(
            @PathVariable Long disputeId,
            @RequestParam String notes,
            Authentication authentication) {
        
        com.group3.accounttrade.entity.User admin = adminDashboardService.getUserByUsername(authentication.getName());
        if (admin == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin not found"));
        }
        
        try {
            disputeService.addAdminNote(disputeId, admin.getUserId(), notes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notes added successfully");
            response.put("disputeId", disputeId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Gets dispute statistics.
     *
     * @return DisputeStats
     */
    @GetMapping("/disputes/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeService.DisputeStats> getDisputeStats() {
        DisputeService.DisputeStats stats = disputeService.getDisputeStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== Transaction History Endpoints ====================

    /**
     * Gets paginated transaction history with optional filters.
     * All transactions are Orders in the system.
     *
     * @param keyword Optional keyword search (order number, buyer username, seller username)
     * @param status Optional status filter (PENDING, COMPLETED, CANCELLED, REFUNDED, etc.)
     * @param from Optional start date filter
     * @param to Optional end date filter
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of transactions
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        java.time.LocalDateTime fromDateTime = null;
        java.time.LocalDateTime toDateTime = null;

        if (from != null && !from.isEmpty()) {
            try {
                fromDateTime = java.time.LocalDate.parse(from).atStartOfDay();
            } catch (Exception e) {
                log.warn("Invalid 'from' date format: {}", from);
            }
        }

        if (to != null && !to.isEmpty()) {
            try {
                toDateTime = java.time.LocalDate.parse(to).atTime(23, 59, 59);
            } catch (Exception e) {
                log.warn("Invalid 'to' date format: {}", to);
            }
        }

        Page<TransactionDTO> transactionsPage = adminTransactionService.getAllTransactions(
                keyword, status, fromDateTime, toDateTime, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", transactionsPage.getContent());
        response.put("totalElements", transactionsPage.getTotalElements());
        response.put("totalPages", transactionsPage.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);

        return ResponseEntity.ok(response);
    }

    /**
     * Gets transaction statistics.
     *
     * @return TransactionStatsDTO
     */
    @GetMapping("/transactions/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionStatsDTO> getTransactionStats() {
        TransactionStatsDTO stats = adminTransactionService.getTransactionStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets detailed transaction information.
     *
     * @param transactionId The transaction ID (format: TXN-TYPE-ID)
     * @return TransactionDTO
     */
    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO> getTransactionDetail(@PathVariable String transactionId) {
        java.util.Optional<TransactionDTO> transaction = adminTransactionService.getTransactionById(transactionId);
        if (transaction.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transaction.get());
    }

    // ==================== Commission Management Endpoints ====================

    @GetMapping("/commission/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommissionConfigDTO> getCommissionConfig() {
        CommissionConfig config = commissionService.getConfig();
        if (config == null) {
            CommissionConfigDTO dto = new CommissionConfigDTO(null, commissionService.getGlobalRate(),
                    java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(100), null, null);
            return ResponseEntity.ok(dto);
        }
        CommissionConfigDTO dto = new CommissionConfigDTO(
                config.getId(),
                config.getGlobalRatePercent(),
                config.getMinRatePercent(),
                config.getMaxRatePercent(),
                config.getUpdatedAt(),
                config.getUpdatedBy() != null ? config.getUpdatedBy().getUsername() : null
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/commission/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateCommissionConfig(
            @RequestParam java.math.BigDecimal rate,
            Authentication authentication) {
        com.group3.accounttrade.entity.User admin = adminDashboardService.getUserByUsername(authentication.getName());
        if (admin == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin not found"));
        }
        try {
            CommissionConfig updated = commissionService.updateGlobalRate(rate, admin);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commission rate updated successfully");
            response.put("newRate", updated.getGlobalRatePercent());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/commission/earnings/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommissionEarningSummaryDTO> getCommissionEarningSummary() {
        CommissionEarningSummaryDTO summary = new CommissionEarningSummaryDTO(
                commissionService.getTodayEarnings(),
                commissionService.getMonthEarnings(),
                commissionService.getTotalEarnings(),
                commissionService.getTotalTransactionCount()
        );
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/commission/earnings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCommissionEarnings(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        java.time.LocalDateTime fromDateTime = null;
        java.time.LocalDateTime toDateTime = null;
        if (from != null && !from.isEmpty()) {
            try { fromDateTime = java.time.LocalDate.parse(from).atStartOfDay(); } catch (Exception ignored) {}
        }
        if (to != null && !to.isEmpty()) {
            try { toDateTime = java.time.LocalDate.parse(to).atTime(23, 59, 59); } catch (Exception ignored) {}
        }

        Page<PlatformEarning> earningsPage = commissionService.getEarnings(fromDateTime, toDateTime, pageable);
        java.util.List<PlatformEarningDTO> content = earningsPage.getContent().stream()
                .map(pe -> new PlatformEarningDTO(
                        pe.getId(),
                        pe.getOrder() != null ? pe.getOrder().getOrderNumber() : null,
                        pe.getCategory() != null ? pe.getCategory().getCategoryName() : null,
                        pe.getGrossAmount(),
                        pe.getCommissionAmount(),
                        pe.getRateApplied(),
                        pe.getCreatedAt()
                ))
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", earningsPage.getTotalElements());
        response.put("totalPages", earningsPage.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories/{categoryId}/commission")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCategoryCommissionRate(@PathVariable Integer categoryId) {
        return adminDashboardService.getCategoryById(categoryId)
                .map(cat -> {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("categoryId", cat.getCategoryId());
                    resp.put("categoryName", cat.getCategoryName());
                    resp.put("commissionRate", cat.getCommissionRate());
                    resp.put("effectiveRate", commissionService.getEffectiveRate(cat));
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/categories/{categoryId}/commission")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateCategoryCommissionRate(
            @PathVariable Integer categoryId,
            @RequestParam(required = false) java.math.BigDecimal rate) {
        return adminDashboardService.getCategoryById(categoryId)
                .map(cat -> {
                    cat.setCommissionRate(rate);
                    adminDashboardService.saveCategory(cat);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", true);
                    resp.put("categoryId", cat.getCategoryId());
                    resp.put("commissionRate", cat.getCommissionRate());
                    resp.put("message", rate == null ? "Category rate cleared (using global rate)" : "Category rate updated");
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
