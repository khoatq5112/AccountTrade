package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findByParentIsNullOrderByDisplayOrderAsc();

    List<Category> findByParentCategoryIdOrderByDisplayOrderAsc(Integer parentId);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.displayOrder ASC")
    List<Category> findAllParentCategories();
}
