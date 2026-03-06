package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.PostStatus;
import com.group3.accounttrade.repository.CategoryRepository;
import com.group3.accounttrade.repository.PostRepository;
import com.group3.accounttrade.repository.PostStatusRepository;
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
    private final PostStatusRepository postStatusRepository;

    @Transactional(readOnly = true)
    public List<Category> getAllParentCategories() {
        return categoryRepository.findByParentIsNullOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Category> getSubcategories(Integer parentId) {
        return categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(parentId);
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<Post> getPostsWithFilters(Integer categoryId, String keyword, Double minPrice, Double maxPrice, Pageable pageable) {
        return postRepository.findPostsWithFilters(categoryId, keyword, minPrice, maxPrice, pageable);
    }

    @Transactional(readOnly = true)
    public List<Post> getPostsByCategoryName(String categoryName, int limit) {
        PostStatus availableStatus = postStatusRepository.findByStatusName("Available").orElse(null);
        if (availableStatus == null) {
            return List.of();
        }

        List<Category> allCategories = categoryRepository.findAll();
        Category foundCategory = null;

        for (Category cat : allCategories) {
            if (cat.getCategoryName().equals(categoryName)) {
                foundCategory = cat;
                break;
            }
        }

        if (foundCategory == null) {
            return List.of();
        }

        if (foundCategory.getParent() == null) {
            return postRepository.findByParentCategory(foundCategory.getCategoryId(), availableStatus, Pageable.ofSize(limit));
        }

        return postRepository.findByCategoryAndStatus(foundCategory, availableStatus, Pageable.ofSize(limit));
    }

    @Transactional(readOnly = true)
    public List<Post> searchPosts(String query) {
        PostStatus availableStatus = postStatusRepository.findByStatusName("Available").orElse(null);
        if (availableStatus == null) {
            return List.of();
        }
        return postRepository.searchByTitle(query, availableStatus);
    }
}
