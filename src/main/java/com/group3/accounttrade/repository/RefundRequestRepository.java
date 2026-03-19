package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.RefundRequest;
import com.group3.accounttrade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RefundRequest entity.
 * Tracks the full lifecycle of refund processing.
 */
@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findByRefundNumber(String refundNumber);

    List<RefundRequest> findByOrder(Order order);

    List<RefundRequest> findByOrderByRequestedAtDesc(Order order);

    List<RefundRequest> findByDispute(Dispute dispute);

    List<RefundRequest> findByRequestedBy(User requestedBy);

    List<RefundRequest> findByRequestedByOrderByRequestedAtDesc(User requestedBy);

    List<RefundRequest> findByProcessedByAdmin(User processedByAdmin);

    List<RefundRequest> findByStatus(String status);

    @Query("SELECT rr FROM RefundRequest rr WHERE rr.status = :status ORDER BY rr.requestedAt ASC")
    List<RefundRequest> findByStatusOrderByRequestedAtAsc(@Param("status") String status);

    @Query("SELECT rr FROM RefundRequest rr WHERE rr.status = 'PENDING' ORDER BY rr.requestedAt ASC")
    List<RefundRequest> findPendingRefundsOrderByRequestedAt();

    @Query("SELECT COUNT(rr) FROM RefundRequest rr WHERE rr.status = 'PENDING'")
    long countPendingRefunds();

    @Query("SELECT COALESCE(SUM(rr.refundAmount), 0) FROM RefundRequest rr WHERE rr.status = 'COMPLETED' AND rr.completedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumCompletedRefundAmountBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT rr FROM RefundRequest rr WHERE rr.order = :order AND rr.status IN ('PENDING', 'APPROVED', 'PROCESSING')")
    List<RefundRequest> findActiveRefundsForOrder(@Param("order") Order order);

    @Query("SELECT rr FROM RefundRequest rr WHERE rr.dispute = :dispute AND rr.status IN ('PENDING', 'APPROVED', 'PROCESSING')")
    List<RefundRequest> findActiveRefundsForDispute(@Param("dispute") Dispute dispute);

    @Query("SELECT rr FROM RefundRequest rr WHERE rr.requestedAt BETWEEN :startDate AND :endDate ORDER BY rr.requestedAt DESC")
    List<RefundRequest> findByRequestedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    List<RefundRequest> findByRefundType(String refundType);

    @Query("SELECT rr FROM RefundRequest rr WHERE rr.status IN :statuses ORDER BY rr.requestedAt DESC")
    List<RefundRequest> findByStatusIn(@Param("statuses") List<String> statuses);

    boolean existsByOrderAndStatusIn(Order order, List<String> statuses);
}
