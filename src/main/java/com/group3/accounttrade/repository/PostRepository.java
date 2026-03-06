package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByCategory(Category category);

    List<Post> findByStatus(PostStatus status);

    List<Post> findByCategoryAndStatus(Category category, PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category.parent.categoryId = :parentId AND p.status = :status")
    List<Post> findByParentCategory(@Param("parentId") Integer parentId, @Param("status") PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status.statusName = 'Available' ORDER BY p.createdAt DESC")
    List<Post> findLatestAvailablePosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status.statusName = 'Available' " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId OR p.category.parent.categoryId = :categoryId) " +
           "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Post> findPostsWithFilters(
            @Param("categoryId") Integer categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.status = :status AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Post> searchByTitle(@Param("query") String query, @Param("status") PostStatus status);
}
