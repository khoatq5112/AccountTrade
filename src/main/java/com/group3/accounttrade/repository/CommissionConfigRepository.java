package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.CommissionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommissionConfigRepository extends JpaRepository<CommissionConfig, Long> {
    Optional<CommissionConfig> findFirstByOrderByIdAsc();
}
