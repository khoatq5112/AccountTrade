package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.EscrowTransaction;
import com.group3.accounttrade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for EscrowTransaction entity.
 * Tracks all escrow movements for audit purposes.
 */
@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {

    List<EscrowTransaction> findByEscrow(Escrow escrow);

    List<EscrowTransaction> findByEscrowOrderByCreatedAtDesc(Escrow escrow);

    @Query("SELECT et FROM EscrowTransaction et WHERE et.escrow.order.orderNumber = :orderNumber ORDER BY et.createdAt DESC")
    List<EscrowTransaction> findByOrderNumberOrderByCreatedAtDesc(String orderNumber);

    List<EscrowTransaction> findByTransactionType(String transactionType);

    @Query("SELECT et FROM EscrowTransaction et WHERE et.initiatedBy = :user ORDER BY et.createdAt DESC")
    List<EscrowTransaction> findByInitiatedBy(User user);

    @Query("SELECT SUM(et.amount) FROM EscrowTransaction et WHERE et.transactionType = :transactionType AND et.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountByTypeAndDateRange(String transactionType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT et FROM EscrowTransaction et WHERE et.createdAt BETWEEN :start AND :end ORDER BY et.createdAt DESC")
    List<EscrowTransaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(et) FROM EscrowTransaction et WHERE et.transactionType = :transactionType")
    long countByTransactionType(String transactionType);
}
