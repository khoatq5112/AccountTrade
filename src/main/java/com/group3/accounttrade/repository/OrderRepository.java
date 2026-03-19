package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.OrderStatus;
import com.group3.accounttrade.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderStatus os " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.post p " +
           "WHERE o.orderId = :orderId")
    Optional<Order> findDetailedByOrderId(Long orderId);

    List<Order> findByBuyer(User buyer);

    Page<Order> findByBuyer(User buyer, Pageable pageable);

    List<Order> findByOrderStatus(OrderStatus orderStatus);

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.buyer = :buyer AND o.orderStatus.statusName IN :statusNames")
    List<Order> findByBuyerAndStatusIn(User buyer, List<String> statusNames);

    @Query("SELECT o FROM Order o WHERE o.buyer = :buyer AND o.orderStatus.statusName IN :statusNames")
    Page<Order> findByBuyerAndStatusIn(User buyer, List<String> statusNames, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.confirmationDeadline < :now AND o.orderStatus.statusName = :statusName")
    List<Order> findExpiredConfirmationOrders(LocalDateTime now, String statusName);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.buyer = :buyer AND o.orderStatus.statusName IN :statusNames")
    long countByBuyerAndStatusIn(User buyer, List<String> statusNames);

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.seller = :seller")
    List<Order> findOrdersContainingSellerPosts(User seller);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.seller = :seller")
    Page<Order> findOrdersContainingSellerPosts(User seller, Pageable pageable);
}
