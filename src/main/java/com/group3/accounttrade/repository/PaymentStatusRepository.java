package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PaymentStatus entity.
 */
@Repository
public interface PaymentStatusRepository extends JpaRepository<PaymentStatus, Integer> {

    Optional<PaymentStatus> findByStatusName(String statusName);

    boolean existsByStatusName(String statusName);
}
