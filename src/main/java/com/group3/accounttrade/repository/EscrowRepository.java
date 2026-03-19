package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.EscrowStatus;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Escrow entity.
 */
@Repository
public interface EscrowRepository extends JpaRepository<Escrow, Long> {

    Optional<Escrow> findByOrder(Order order);

    Optional<Escrow> findByOrderOrderNumber(String orderNumber);

    List<Escrow> findByEscrowStatus(EscrowStatus escrowStatus);

    Page<Escrow> findByEscrowStatus(EscrowStatus escrowStatus, Pageable pageable);

    @Query("SELECT e FROM Escrow e WHERE e.escrowStatus.statusName IN :statusNames")
    List<Escrow> findByStatusIn(List<String> statusNames);

    @Query("SELECT e FROM Escrow e WHERE e.escrowStatus.statusName = :statusName AND e.autoReleaseDeadline < :now")
    List<Escrow> findExpiredEscrows(String statusName, LocalDateTime now);

    @Query("SELECT SUM(e.amount) FROM Escrow e WHERE e.escrowStatus.statusName = :statusName")
    BigDecimal sumTotalAmountByStatus(String statusName);

    @Query("SELECT COUNT(e) FROM Escrow e WHERE e.escrowStatus.statusName = :statusName")
    long countByStatusName(String statusName);

    @Query("SELECT e FROM Escrow e WHERE e.createdAt BETWEEN :start AND :end")
    List<Escrow> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByOrder(Order order);

    /**
     * Find escrows for a specific seller by joining through order items.
     */
    @Query("SELECT DISTINCT e FROM Escrow e JOIN e.order.orderItems oi WHERE oi.post.seller = :seller")
    List<Escrow> findBySeller(User seller);

    @Query("SELECT DISTINCT e FROM Escrow e JOIN e.order.orderItems oi WHERE oi.post.seller = :seller")
    Page<Escrow> findBySeller(User seller, Pageable pageable);

    /**
     * Find escrows for a seller with specific status.
     */
    @Query("SELECT DISTINCT e FROM Escrow e JOIN e.order.orderItems oi WHERE oi.post.seller = :seller AND e.escrowStatus.statusName IN :statusNames")
    List<Escrow> findBySellerAndStatusIn(User seller, List<String> statusNames);

    /**
     * Sum escrow amounts for a seller with specific status.
     */
    @Query("SELECT SUM(e.amount) FROM Escrow e JOIN e.order.orderItems oi WHERE oi.post.seller = :seller AND e.escrowStatus.statusName = :statusName")
    BigDecimal sumAmountBySellerAndStatus(User seller, String statusName);

    /**
     * Count escrows for a seller with specific status.
     */
    @Query("SELECT COUNT(DISTINCT e) FROM Escrow e JOIN e.order.orderItems oi WHERE oi.post.seller = :seller AND e.escrowStatus.statusName = :statusName")
    long countBySellerAndStatus(User seller, String statusName);
}
