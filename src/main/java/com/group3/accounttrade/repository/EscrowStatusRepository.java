package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for EscrowStatus entity.
 */
@Repository
public interface EscrowStatusRepository extends JpaRepository<EscrowStatus, Integer> {

    Optional<EscrowStatus> findByStatusName(String statusName);

    boolean existsByStatusName(String statusName);
}
