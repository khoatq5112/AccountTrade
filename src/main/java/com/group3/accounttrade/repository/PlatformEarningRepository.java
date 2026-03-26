package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.PlatformEarning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface PlatformEarningRepository extends JpaRepository<PlatformEarning, Long> {

    @Query("SELECT COALESCE(SUM(pe.commissionAmount), 0) FROM PlatformEarning pe WHERE pe.createdAt BETWEEN :from AND :to")
    BigDecimal sumCommissionAmountByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(pe.commissionAmount), 0) FROM PlatformEarning pe")
    BigDecimal sumCommissionAmountAll();

    @Query("SELECT COUNT(pe) FROM PlatformEarning pe WHERE pe.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(pe) FROM PlatformEarning pe")
    long countAll();

    Page<PlatformEarning> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT pe FROM PlatformEarning pe WHERE pe.createdAt BETWEEN :from AND :to ORDER BY pe.createdAt DESC")
    Page<PlatformEarning> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
