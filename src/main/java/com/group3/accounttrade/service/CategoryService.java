package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.StockStatus;
import com.group3.accounttrade.repository.CategoryRepository;
import com.group3.accounttrade.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    /**
     * Get all categories ordered by display order.
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Post getPostById(Integer id) {
        return postRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<Post> getPostsWithFilters(Integer categoryId, String keyword, Double minPrice, Double maxPrice, Pageable pageable) {
        return postRepository.findPostsWithFilters(categoryId, keyword, minPrice, maxPrice, pageable);
    }

    @Transactional(readOnly = true)
    public List<Post> getPostsByCategoryName(String categoryName, int limit) {
        // Find category by name
        Category foundCategory = categoryRepository.findAll().stream()
                .filter(cat -> cat.getCategoryName().equals(categoryName))
                .findFirst()
                .orElse(null);

        if (foundCategory == null) {
            return List.of();
        }

        return postRepository.findByCategoryAndStockStatus(foundCategory, StockStatus.IN_STOCK, Pageable.ofSize(limit)).getContent();
    }

    @Transactional(readOnly = true)
    public List<Post> searchPosts(String query) {
        return postRepository.searchByTitle(query);
    }
}
