package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for OrderStatus entity.
 */
@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatus, Integer> {

    Optional<OrderStatus> findByStatusName(String statusName);

    boolean existsByStatusName(String statusName);
}
