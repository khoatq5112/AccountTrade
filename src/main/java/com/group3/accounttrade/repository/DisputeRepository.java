package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.DisputeStatus;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Dispute entity.
 * Handles dispute tracking and resolution.
 */
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByDisputeNumber(String disputeNumber);

    List<Dispute> findByOrder(Order order);

    @Query("SELECT d FROM Dispute d WHERE d.order.orderNumber = :orderNumber")
    Optional<Dispute> findByOrderNumber(String orderNumber);

    List<Dispute> findByOpenedBy(User openedBy);

    List<Dispute> findByOpenedByOrderByOpenedAtDesc(User openedBy);

    List<Dispute> findByRespondent(User respondent);

    List<Dispute> findByRespondentOrderByOpenedAtDesc(User respondent);

    List<Dispute> findByDisputeStatus(DisputeStatus disputeStatus);

    Page<Dispute> findByDisputeStatus(DisputeStatus disputeStatus, Pageable pageable);

    @Query("SELECT d FROM Dispute d WHERE d.disputeStatus IN :statuses ORDER BY d.openedAt DESC")
    List<Dispute> findByDisputeStatusIn(@Param("statuses") List<DisputeStatus> statuses);

    @Query("SELECT d FROM Dispute d WHERE d.disputeStatus = :status AND d.openedAt < :threshold")
    List<Dispute> findByDisputeStatusAndOpenedAtBefore(DisputeStatus status, LocalDateTime threshold);

    @Query("SELECT d FROM Dispute d WHERE d.disputeStatus.statusName = 'OPENED' AND d.sellerResponseDeadline < :now")
    List<Dispute> findOverdueSellerResponses(LocalDateTime now);

    @Query("SELECT d FROM Dispute d WHERE d.disputeStatus.statusName = 'UNDER_REVIEW' AND d.resolvedAt IS NULL AND d.openedAt < :threshold")
    List<Dispute> findOverdueResolutions(LocalDateTime threshold);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.openedBy = :user AND d.disputeStatus.statusName NOT IN ('RESOLVED', 'CANCELLED')")
    long countActiveDisputesByOpener(@Param("user") User user);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.respondent = :user AND d.disputeStatus.statusName NOT IN ('RESOLVED', 'CANCELLED')")
    long countActiveDisputesByRespondent(@Param("user") User user);

    @Query("SELECT d FROM Dispute d WHERE " +
           "(:status IS NULL OR d.disputeStatus = :status) AND " +
           "(:openedById IS NULL OR d.openedBy.userId = :openedById) AND " +
           "(:searchTerm IS NULL OR d.disputeNumber LIKE %:searchTerm% OR d.order.orderNumber LIKE %:searchTerm%)")
    Page<Dispute> searchDisputes(@Param("status") DisputeStatus status,
                                  @Param("openedById") Long openedById,
                                  @Param("searchTerm") String searchTerm,
                                  Pageable pageable);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.openedAt BETWEEN :start AND :end")
    long countByOpenedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT d.disputeType, COUNT(d) FROM Dispute d GROUP BY d.disputeType")
    List<Object[]> countByDisputeType();

    boolean existsByOrderAndDisputeStatusIn(Order order, List<DisputeStatus> statuses);

    List<Dispute> findByAssignedAdmin(User admin);

    List<Dispute> findByAssignedAdminOrderByOpenedAtDesc(User admin);

    @Query("SELECT d FROM Dispute d WHERE d.assignedAdmin = :admin AND d.disputeStatus.statusName = 'UNDER_REVIEW'")
    List<Dispute> findPendingReviewByAdmin(@Param("admin") User admin);

    // Paginated queries for buyer disputes with status filter
    @Query("SELECT d FROM Dispute d WHERE d.openedBy = :buyer " +
           "AND (:status IS NULL OR d.disputeStatus.statusName = :status) " +
           "ORDER BY d.openedAt DESC")
    Page<Dispute> findByBuyerWithStatus(@Param("buyer") User buyer,
                                        @Param("status") String status,
                                        Pageable pageable);

    // Paginated queries for seller disputes with status filter
    @Query("SELECT d FROM Dispute d WHERE d.respondent = :seller " +
           "AND (:status IS NULL OR d.disputeStatus.statusName = :status) " +
           "ORDER BY d.openedAt DESC")
    Page<Dispute> findBySellerWithStatus(@Param("seller") User seller,
                                         @Param("status") String status,
                                         Pageable pageable);

    // Count disputes by status names
    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.disputeStatus.statusName IN :statusNames")
    long countByStatusNameIn(@Param("statusNames") List<String> statusNames);

    // Get all disputes for admin with optional status filter
    @Query("SELECT d FROM Dispute d WHERE " +
           "(:status IS NULL OR d.disputeStatus.statusName = :status) " +
           "ORDER BY d.openedAt DESC")
    Page<Dispute> findAllWithStatus(@Param("status") String status, Pageable pageable);
}
