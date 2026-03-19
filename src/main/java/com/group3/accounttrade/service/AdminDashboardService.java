package com.group3.accounttrade.service;

import com.group3.accounttrade.dto.AdminDashboardStats;
import com.group3.accounttrade.dto.ChartDataPoint;
import com.group3.accounttrade.dto.DisputeDTO;
import com.group3.accounttrade.dto.PendingPostDTO;
import com.group3.accounttrade.dto.UserDTO;
import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for gathering admin dashboard statistics and data.
 * Provides aggregated platform metrics, pending approvals, and dispute management data.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final EscrowRepository escrowRepository;
    private final DisputeRepository disputeRepository;
    private final DisputeStatusRepository disputeStatusRepository;
    private final PostRepository postRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * Gets aggregated dashboard statistics for admin overview.
     *
     * @return AdminDashboardStats containing all platform metrics
     */
    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);
        LocalDateTime lastMonthStart = monthStart.minusMonths(1);

        // Daily transaction volume
        BigDecimal todayVolume = calculateVolume(todayStart, now);
        BigDecimal yesterdayVolume = calculateVolume(yesterdayStart, todayStart);
        double volumeChangePercent = calculatePercentChange(todayVolume, yesterdayVolume);

        // Escrow holdings
        BigDecimal escrowHoldings = escrowRepository.sumTotalAmountByStatus(EscrowStatus.HOLDING);
        if (escrowHoldings == null) {
            escrowHoldings = BigDecimal.ZERO;
        }
        long escrowCount = escrowRepository.countByStatusName(EscrowStatus.HOLDING);

        // Monthly fee revenue
        BigDecimal monthlyRevenue = calculateFeeRevenue(monthStart, now);
        BigDecimal lastMonthRevenue = calculateFeeRevenue(lastMonthStart, monthStart);
        double revenueChangePercent = calculatePercentChange(monthlyRevenue, lastMonthRevenue);

        // Pending counts
        long pendingDisputeCount = getPendingDisputeCount();
        long pendingApprovalCount = getPendingApprovalCount();

        return new AdminDashboardStats(
                todayVolume != null ? todayVolume : BigDecimal.ZERO,
                volumeChangePercent,
                escrowHoldings,
                escrowCount,
                monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO,
                revenueChangePercent,
                pendingDisputeCount,
                pendingApprovalCount
        );
    }

    /**
     * Gets posts pending admin approval.
     * Uses OUT_OF_STOCK status as a proxy for pending approval (newly created posts).
     * In a real system, you would have a dedicated PENDING_APPROVAL status.
     *
     * @param limit maximum number of posts to return
     * @return list of pending posts
     */
    @Transactional(readOnly = true)
    public List<PendingPostDTO> getPendingApprovals(int limit) {
        // Get recent OUT_OF_STOCK posts that might need approval
        // This is a simplified approach - in production, use a proper approval workflow
        List<Post> recentPosts = postRepository.findByStockStatus(StockStatus.OUT_OF_STOCK)
                .stream()
                .limit(limit)
                .toList();
        
        return recentPosts.stream()
                .map(post -> new PendingPostDTO(
                        post.getPostId(),
                        post.getTitle(),
                        post.getSeller() != null ? post.getSeller().getUsername() : "Unknown",
                        post.getPrice(),
                        post.getCategory() != null ? post.getCategory().getCategoryName() : "N/A",
                        post.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Gets disputes requiring admin attention.
     *
     * @param limit maximum number of disputes to return
     * @return list of pending disputes
     */
    @Transactional(readOnly = true)
    public List<DisputeDTO> getPendingDisputes(int limit) {
        DisputeStatus openedStatus = disputeStatusRepository.findByStatusName(DisputeStatus.OPENED)
                .orElse(null);
        DisputeStatus underReviewStatus = disputeStatusRepository.findByStatusName(DisputeStatus.UNDER_REVIEW)
                .orElse(null);

        List<DisputeStatus> statuses = new ArrayList<>();
        if (openedStatus != null) {
            statuses.add(openedStatus);
        }
        if (underReviewStatus != null) {
            statuses.add(underReviewStatus);
        }

        if (statuses.isEmpty()) {
            return List.of();
        }

        List<Dispute> disputes = disputeRepository.findByDisputeStatusIn(statuses);
        
        return disputes.stream()
                .limit(limit)
                .map(this::mapToDisputeDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets transaction volume chart data for the specified number of days.
     *
     * @param days number of days to include
     * @return list of chart data points
     */
    @Transactional(readOnly = true)
    public List<ChartDataPoint> getTransactionVolumeChartData(int days) {
        LocalDateTime now = LocalDateTime.now();
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i).toLocalDate();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            BigDecimal dayVolume = calculateVolume(dayStart, dayEnd);
            
            dataPoints.add(new ChartDataPoint(
                    date.format(formatter),
                    dayVolume != null ? dayVolume : BigDecimal.ZERO
            ));
        }

        return dataPoints;
    }

    /**
     * Approves a pending post by setting it to IN_STOCK status.
     *
     * @param postId the post ID to approve
     * @return true if successful
     */
    @Transactional
    public boolean approvePost(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        // Set to IN_STOCK to make it available for purchase
        post.setStockStatus(StockStatus.IN_STOCK);
        postRepository.save(post);
        return true;
    }

    /**
     * Rejects a pending post by keeping it OUT_OF_STOCK.
     * In a real system, you might want to add a DISCONTINUED status.
     *
     * @param postId the post ID to reject
     * @return true if successful
     */
    @Transactional
    public boolean rejectPost(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        // Keep as OUT_OF_STOCK - in production, you might want to delete or mark as rejected
        post.setStockStatus(StockStatus.OUT_OF_STOCK);
        postRepository.save(post);
        return true;
    }

    // Helper methods

    private BigDecimal calculateVolume(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        return orders.stream()
                .filter(o -> "PAID".equals(o.getOrderStatus().getStatusName()) ||
                             "COMPLETED".equals(o.getOrderStatus().getStatusName()) ||
                             "ESCROW".equals(o.getOrderStatus().getStatusName()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateFeeRevenue(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        return orders.stream()
                .filter(o -> "COMPLETED".equals(o.getOrderStatus().getStatusName()))
                .map(o -> o.getPlatformFee() != null ? o.getPlatformFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double calculatePercentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        if (current == null) {
            return -100.0;
        }
        BigDecimal change = current.subtract(previous);
        BigDecimal percentChange = change.multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
        return percentChange.doubleValue();
    }

    private long getPendingDisputeCount() {
        DisputeStatus openedStatus = disputeStatusRepository.findByStatusName(DisputeStatus.OPENED)
                .orElse(null);
        DisputeStatus underReviewStatus = disputeStatusRepository.findByStatusName(DisputeStatus.UNDER_REVIEW)
                .orElse(null);

        long count = 0;
        if (openedStatus != null) {
            count += disputeRepository.findByDisputeStatus(openedStatus).size();
        }
        if (underReviewStatus != null) {
            count += disputeRepository.findByDisputeStatus(underReviewStatus).size();
        }
        return count;
    }

    private long getPendingApprovalCount() {
        // Count OUT_OF_STOCK posts as pending approval (simplified approach)
        return postRepository.countByStockStatus(StockStatus.OUT_OF_STOCK);
    }

    private DisputeDTO mapToDisputeDTO(Dispute dispute) {
        Order order = dispute.getOrder();
        return new DisputeDTO(
                dispute.getDisputeId(),
                dispute.getDisputeNumber(),
                order != null ? order.getOrderNumber() : "N/A",
                order != null ? order.getTotalAmount() : BigDecimal.ZERO,
                dispute.getReason(),
                dispute.getOpenedBy() != null ? dispute.getOpenedBy().getUsername() : "N/A",
                dispute.getRespondent() != null ? dispute.getRespondent().getUsername() : "N/A",
                dispute.getOpenedAt()
        );
    }

    /**
     * Gets recent users for admin user management section.
     *
     * @param limit maximum number of users to return
     * @return list of recent users
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getRecentUsers(int limit) {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        
        return users.stream()
                .limit(limit)
                .map(user -> new UserDTO(
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole() != null ? user.getRole().getRoleName() : "N/A",
                        user.getIsActive() != null ? user.getIsActive() : false,
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
