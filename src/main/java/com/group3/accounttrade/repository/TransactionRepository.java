package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Transaction;
import com.group3.accounttrade.entity.TransactionStatus;
import com.group3.accounttrade.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByBuyerOrderByCreatedAtDesc(User buyer);

    // Seller queries
    List<Transaction> findBySellerOrderByCreatedAtDesc(User seller);

    List<Transaction> findBySellerAndStatusOrderByCreatedAtDesc(User seller, TransactionStatus status);

    List<Transaction> findBySellerAndStatus(User seller, TransactionStatus status);

    long countBySellerAndStatus(User seller, TransactionStatus status);

    // Revenue queries
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.seller = :seller AND t.status.statusName = :statusName")
    BigDecimal sumRevenueBySellerAndStatusName(@Param("seller") User seller, @Param("statusName") String statusName);

    // Sum amount by seller and status (for escrow calculations)
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.seller = :seller AND t.status.statusName = :statusName")
    BigDecimal sumAmountBySellerAndStatus(@Param("seller") User seller, @Param("statusName") String statusName);

    // Convenience method for revenue
    default BigDecimal sumRevenueBySellerAndStatus(User seller, String statusName) {
        return sumRevenueBySellerAndStatusName(seller, statusName);
    }

    // Paginated seller queries
    List<Transaction> findBySellerAndStatusOrderByCreatedAtDesc(User seller, TransactionStatus status, Pageable pageable);

    // Find by seller and multiple statuses
    @Query("SELECT t FROM Transaction t WHERE t.seller = :seller AND t.status IN :statuses ORDER BY t.createdAt DESC")
    List<Transaction> findBySellerAndStatusInOrderByCreatedAtDesc(@Param("seller") User seller, @Param("statuses") List<TransactionStatus> statuses, Pageable pageable);

    // Count by seller
    long countBySeller(User seller);
}
