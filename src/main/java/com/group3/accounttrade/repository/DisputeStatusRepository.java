package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DisputeStatus entity.
 */
@Repository
public interface DisputeStatusRepository extends JpaRepository<DisputeStatus, Integer> {

    Optional<DisputeStatus> findByStatusName(String statusName);

    boolean existsByStatusName(String statusName);
}
