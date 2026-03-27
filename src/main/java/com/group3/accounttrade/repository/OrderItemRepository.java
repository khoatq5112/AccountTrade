package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.OrderItem;
import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for OrderItem entity.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    Optional<OrderItem> findByOrderAndPost(Order order, Post post);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.buyer.id = :buyerId")
    List<OrderItem> findByBuyerId(Long buyerId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.post.seller.id = :sellerId")
    List<OrderItem> findBySellerId(Long sellerId);

    @EntityGraph(attributePaths = {"order", "order.buyer", "order.orderStatus", "post", "seller"})
    @Query(value = """
            SELECT oi FROM OrderItem oi
            JOIN oi.order o
            JOIN o.buyer b
            JOIN o.orderStatus os
            WHERE oi.seller = :seller
              AND UPPER(os.statusName) <> 'PAYMENT_FAILED'
              AND (:status IS NULL OR :status = '' OR LOWER(os.statusName) = LOWER(:status))
              AND (
                    :keyword IS NULL OR :keyword = '' OR
                    LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(b.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(oi.postTitleSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(oi) FROM OrderItem oi
            JOIN oi.order o
            JOIN o.buyer b
            JOIN o.orderStatus os
            WHERE oi.seller = :seller
              AND UPPER(os.statusName) <> 'PAYMENT_FAILED'
              AND (:status IS NULL OR :status = '' OR LOWER(os.statusName) = LOWER(:status))
              AND (
                    :keyword IS NULL OR :keyword = '' OR
                    LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(b.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                    LOWER(oi.postTitleSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<OrderItem> findSellerOrderItems(User seller, String keyword, String status, Pageable pageable);

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.post = :post AND oi.order.orderStatus.statusName IN :statusNames")
    long countByPostAndOrderStatusIn(Post post, List<String> statusNames);
}
