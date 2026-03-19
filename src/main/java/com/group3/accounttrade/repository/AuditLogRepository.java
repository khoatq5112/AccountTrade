package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.AuditLog;
import com.group3.accounttrade.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity.
 * Provides comprehensive audit trail for compliance and debugging.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPerformedBy(User performedBy);

    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(User performedBy);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("SELECT al FROM AuditLog al WHERE al.entityType = :entityType AND al.entityId = :entityId ORDER BY al.createdAt DESC")
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    List<AuditLog> findByAction(String action);

    @Query("SELECT al FROM AuditLog al WHERE al.action = :action ORDER BY al.createdAt DESC")
    List<AuditLog> findByActionOrderByCreatedAtDesc(@Param("action") String action);

    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<AuditLog> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.performedBy = :performedBy AND al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<AuditLog> findByPerformedByAndCreatedAtBetween(
            @Param("performedBy") User performedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.entityType IN :entityTypes ORDER BY al.createdAt DESC")
    List<AuditLog> findByEntityTypeIn(@Param("entityTypes") List<String> entityTypes);

    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress ORDER BY al.createdAt DESC")
    List<AuditLog> findByIpAddressOrderByCreatedAtDesc(@Param("ipAddress") String ipAddress);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.performedBy = :performedBy AND al.action = :action AND al.createdAt > :since")
    long countByPerformedByAndActionSince(
            @Param("performedBy") User performedBy,
            @Param("action") String action,
            @Param("since") LocalDateTime since);

    @Query("SELECT al FROM AuditLog al WHERE al.performedBy = :performedBy AND al.action LIKE :actionPattern ORDER BY al.createdAt DESC")
    List<AuditLog> findByPerformedByAndActionLike(
            @Param("performedBy") User performedBy,
            @Param("actionPattern") String actionPattern);

    List<AuditLog> findByEventType(String eventType);

    @Query("SELECT al FROM AuditLog al WHERE al.eventType = :eventType ORDER BY al.createdAt DESC")
    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(@Param("eventType") String eventType);

    @Query("SELECT al FROM AuditLog al WHERE al.success = false ORDER BY al.createdAt DESC")
    List<AuditLog> findFailedActionsOrderByCreatedAtDesc();
}
