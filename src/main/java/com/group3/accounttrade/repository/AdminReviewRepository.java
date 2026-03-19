package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.AdminReview;
import com.group3.accounttrade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AdminReview entity.
 * Tracks manual admin intervention and review actions.
 */
@Repository
public interface AdminReviewRepository extends JpaRepository<AdminReview, Long> {

    List<AdminReview> findByReviewedByAdmin(User reviewedByAdmin);

    List<AdminReview> findByReviewedByAdminOrderByCreatedAtDesc(User reviewedByAdmin);

    List<AdminReview> findByReviewType(String reviewType);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.reviewType = :reviewType ORDER BY ar.createdAt DESC")
    List<AdminReview> findByReviewTypeOrderByCreatedAtDesc(@Param("reviewType") String reviewType);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.entityType = :entityType AND ar.entityId = :entityId ORDER BY ar.createdAt DESC")
    List<AdminReview> findByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.entityType = :entityType AND ar.entityId = :entityId AND ar.reviewType = :reviewType ORDER BY ar.createdAt DESC")
    List<AdminReview> findByEntityAndReviewType(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("reviewType") String reviewType);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.status = :status ORDER BY ar.createdAt DESC")
    List<AdminReview> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.status = 'PENDING' ORDER BY ar.priority DESC, ar.createdAt ASC")
    List<AdminReview> findPendingReviewsOrderByPriority();

    @Query("SELECT ar FROM AdminReview ar WHERE ar.status = 'PENDING' AND ar.reviewType = :reviewType ORDER BY ar.priority DESC, ar.createdAt ASC")
    List<AdminReview> findPendingReviewsByType(@Param("reviewType") String reviewType);

    @Query("SELECT COUNT(ar) FROM AdminReview ar WHERE ar.status = 'PENDING'")
    long countPendingReviews();

    @Query("SELECT COUNT(ar) FROM AdminReview ar WHERE ar.status = 'PENDING' AND ar.reviewType = :reviewType")
    long countPendingReviewsByType(@Param("reviewType") String reviewType);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.entityType = :entityType AND ar.entityId = :entityId AND ar.status = 'PENDING'")
    Optional<AdminReview> findPendingReviewForEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    List<AdminReview> findByPriority(Integer priority);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.priority >= :minPriority AND ar.status = 'PENDING' ORDER BY ar.priority DESC, ar.createdAt ASC")
    List<AdminReview> findHighPriorityPendingReviews(@Param("minPriority") Integer minPriority);

    List<AdminReview> findByAssignedAdmin(User assignedAdmin);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.assignedAdmin = :assignedAdmin AND ar.status = 'PENDING' ORDER BY ar.priority DESC, ar.createdAt ASC")
    List<AdminReview> findPendingReviewsByAssignedAdmin(@Param("assignedAdmin") User assignedAdmin);

    @Query("SELECT ar FROM AdminReview ar WHERE ar.status = 'PENDING' AND ar.slaDeadline < CURRENT_TIMESTAMP")
    List<AdminReview> findOverduePendingReviews();
}
