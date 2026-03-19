package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.DisputeMessage;
import com.group3.accounttrade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DisputeMessage entity.
 * Tracks all communication within a dispute.
 */
@Repository
public interface DisputeMessageRepository extends JpaRepository<DisputeMessage, Long> {

    List<DisputeMessage> findByDispute(Dispute dispute);

    List<DisputeMessage> findByDisputeOrderByCreatedAtAsc(Dispute dispute);

    @Query("SELECT dm FROM DisputeMessage dm WHERE dm.dispute.disputeNumber = :disputeNumber ORDER BY dm.createdAt ASC")
    List<DisputeMessage> findByDisputeNumberOrderByCreatedAtAsc(String disputeNumber);

    List<DisputeMessage> findBySender(User sender);

    List<DisputeMessage> findBySenderOrderByCreatedAtDesc(User sender);

    @Query("SELECT dm FROM DisputeMessage dm WHERE dm.dispute = :dispute AND dm.isInternal = false ORDER BY dm.createdAt ASC")
    List<DisputeMessage> findPublicMessagesByDispute(@Param("dispute") Dispute dispute);

    @Query("SELECT dm FROM DisputeMessage dm WHERE dm.dispute = :dispute AND dm.isInternal = true ORDER BY dm.createdAt ASC")
    List<DisputeMessage> findInternalMessagesByDispute(@Param("dispute") Dispute dispute);

    @Query("SELECT COUNT(dm) FROM DisputeMessage dm WHERE dm.dispute = :dispute AND dm.readAt IS NULL AND dm.sender != :user")
    long countUnreadMessagesByDisputeAndNotSender(@Param("dispute") Dispute dispute, @Param("user") User user);

    @Query("SELECT dm FROM DisputeMessage dm WHERE dm.dispute = :dispute AND dm.sender != :user AND dm.readAt IS NULL")
    List<DisputeMessage> findUnreadMessagesByDisputeAndNotSender(@Param("dispute") Dispute dispute, @Param("user") User user);
}
