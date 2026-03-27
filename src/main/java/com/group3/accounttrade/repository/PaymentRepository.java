package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    @EntityGraph(attributePaths = {
        "order",
        "order.buyer",
        "order.orderItems",
        "order.orderItems.seller",
        "paymentStatus"
    })
    Optional<Payment> findByOrder(Order order);

    @EntityGraph(attributePaths = {
        "order",
        "order.buyer",
        "order.orderItems",
        "order.orderItems.seller",
        "paymentStatus"
    })
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.vnpayTxnRef = :vnpayTxnRef")
    @EntityGraph(attributePaths = {
        "order",
        "order.buyer",
        "order.orderItems",
        "order.orderItems.seller",
        "paymentStatus"
    })
    Optional<Payment> findLockedByVnpayTxnRef(@Param("vnpayTxnRef") String vnpayTxnRef);

    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus.statusName IN :statusNames")
    List<Payment> findByStatusIn(List<String> statusNames);

    @Query("SELECT p FROM Payment p WHERE p.order.buyer = :buyerId ORDER BY p.createdAt DESC")
    List<Payment> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :start AND :end")
    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus.statusName = :statusName")
    long countByStatusName(String statusName);

    boolean existsByVnpayTxnRef(String vnpayTxnRef);

    boolean existsByPaymentId(String paymentId);

    /**
     * Override findById to load all related entities eagerly using EntityGraph.
     * This handles null relationships gracefully unlike JOIN FETCH.
     */
    @Override
    @EntityGraph(attributePaths = {
        "order", 
        "order.buyer", 
        "order.orderItems", 
        "order.orderItems.post", 
        "order.orderItems.seller",
        "paymentStatus"
    })
    Optional<Payment> findById(Long id);
}
