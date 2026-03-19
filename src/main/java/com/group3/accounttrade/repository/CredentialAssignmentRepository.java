package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.CredentialAssignment;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.PostCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CredentialAssignment entity.
 * Tracks which credentials are assigned to which orders.
 */
@Repository
public interface CredentialAssignmentRepository extends JpaRepository<CredentialAssignment, Long> {

    List<CredentialAssignment> findByOrderItem(OrderItem orderItem);

    List<CredentialAssignment> findByCredentialOrderByAssignedAtDesc(PostCredential credential);

    List<CredentialAssignment> findByOrderItemOrderByAssignedAtDesc(OrderItem orderItem);

    Optional<CredentialAssignment> findByOrderItemAndCredential(OrderItem orderItem, PostCredential credential);

    @Query("SELECT ca FROM CredentialAssignment ca WHERE ca.orderItem.order.orderNumber = :orderNumber")
    List<CredentialAssignment> findByOrderNumber(String orderNumber);

    @Query("SELECT ca FROM CredentialAssignment ca WHERE ca.credential = :credential AND ca.assignmentStatus IN :statuses")
    List<CredentialAssignment> findByCredentialAndStatusIn(@Param("credential") PostCredential credential, 
                                                            @Param("statuses") List<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ca FROM CredentialAssignment ca WHERE ca.credential = :credential AND ca.assignmentStatus = :status")
    List<CredentialAssignment> findByCredentialAndStatusWithLock(@Param("credential") PostCredential credential, 
                                                                  @Param("status") String status);

    @Query("SELECT COUNT(ca) > 0 FROM CredentialAssignment ca WHERE ca.credential = :credential AND ca.assignmentStatus IN :statuses")
    boolean existsByCredentialAndStatusIn(@Param("credential") PostCredential credential, 
                                           @Param("statuses") List<String> statuses);

    @Query("SELECT ca FROM CredentialAssignment ca WHERE ca.assignmentStatus = :status AND ca.assignedAt < :threshold")
    List<CredentialAssignment> findByStatusAndAssignedAtBefore(String status, LocalDateTime threshold);

    @Modifying
    @Query("UPDATE CredentialAssignment ca SET ca.assignmentStatus = :newStatus WHERE ca.assignmentId = :assignmentId AND ca.assignmentStatus = :oldStatus")
    int updateStatus(@Param("assignmentId") Long assignmentId, 
                     @Param("oldStatus") String oldStatus, 
                     @Param("newStatus") String newStatus);

    @Query("SELECT ca FROM CredentialAssignment ca JOIN ca.orderItem oi JOIN oi.order o WHERE o.buyer.userId = :buyerId ORDER BY ca.assignedAt DESC")
    List<CredentialAssignment> findByBuyerIdOrderByAssignedAtDesc(Long buyerId);

    @Query("SELECT ca FROM CredentialAssignment ca JOIN ca.credential c JOIN c.post p WHERE p.seller.userId = :sellerId ORDER BY ca.assignedAt DESC")
    List<CredentialAssignment> findBySellerIdOrderByAssignedAtDesc(Long sellerId);

    long countByAssignmentStatus(String status);

    @Query("SELECT ca FROM CredentialAssignment ca WHERE ca.assignmentStatus = :status AND ca.deliveredAt IS NOT NULL AND ca.deliveredAt < :threshold")
    List<CredentialAssignment> findDeliveredBeforeThreshold(String status, LocalDateTime threshold);
}
