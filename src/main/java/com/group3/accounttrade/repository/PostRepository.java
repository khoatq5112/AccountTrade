package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.StockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByCategory(Category category);

    List<Post> findByStockStatus(StockStatus stockStatus);

    Page<Post> findByCategoryAndStockStatus(Category category, StockStatus stockStatus, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category = :category " +
           "ORDER BY CASE WHEN p.stockStatus = 'IN_STOCK' THEN 0 ELSE 1 END, p.createdAt DESC")
    Page<Post> findByCategoryOrderByAvailability(@Param("category") Category category, Pageable pageable);

    @Query("SELECT p FROM Post p ORDER BY CASE WHEN p.stockStatus = 'IN_STOCK' THEN 0 ELSE 1 END, p.createdAt DESC")
    List<Post> findLatestAvailablePosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE " +
           "(:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
           "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findPostsWithFilters(
            @Param("categoryId") Integer categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Post> searchByTitle(@Param("query") String query);

    @Query("SELECT p FROM Post p WHERE p.seller.username = :username " +
           "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
           "AND (:stockStatus IS NULL OR p.stockStatus = :stockStatus)")
    Page<Post> findSellerPostsWithFilters(
            @Param("username") String username,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("stockStatus") StockStatus stockStatus,
            Pageable pageable);

    Optional<Post> findByPostIdAndSeller_Username(Integer postId, String username);

    Page<Post> findBySeller_UsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    long countBySeller_Username(String username);

    long countBySeller_UsernameAndStockStatus(String username, StockStatus stockStatus);

    long countByStockStatus(StockStatus stockStatus);

    @Modifying
    @Query("UPDATE Post p SET p.stockStatus = :status WHERE p.postId = :postId")
    void updateStatus(@Param("postId") Integer postId, @Param("status") StockStatus status);

    /**
     * Find posts by stock status with pagination for admin approval workflow.
     */
    List<Post> findByStockStatus(StockStatus stockStatus, Pageable pageable);
}
