package com.group3.accounttrade.controller;

import com.group3.accounttrade.entity.Category;
import com.group3.accounttrade.entity.Post;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MarketplaceController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    @GetMapping("/marketplace")
    public String marketplace(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        // Get all parent categories for sidebar
        List<Category> parentCategories = categoryService.getAllParentCategories();
        model.addAttribute("parentCategories", parentCategories);

        // Get current category
        Category currentCategory = null;
        if (categoryId != null) {
            currentCategory = categoryService.getCategoryById(categoryId);
        }
        model.addAttribute("currentCategory", currentCategory);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", q);
        model.addAttribute("sort", sort);

        // Parse price filters
        Double minPriceValue = null;
        Double maxPriceValue = null;
        try {
            if (minPrice != null && !minPrice.isEmpty()) {
                minPriceValue = Double.parseDouble(minPrice);
            }
            if (maxPrice != null && !maxPrice.isEmpty()) {
                maxPriceValue = Double.parseDouble(maxPrice);
            }
        } catch (NumberFormatException e) {
            // Ignore invalid price values
        }

        // Determine sort
        Sort sortOption;
        switch (sort) {
            case "price_asc":
                sortOption = Sort.by("price").ascending();
                break;
            case "price_desc":
                sortOption = Sort.by("price").descending();
                break;
            default:
                sortOption = Sort.by("createdAt").descending();
        }

        // Get paginated posts
        Pageable pageable = PageRequest.of(page, size, sortOption);
        var result = categoryService.getPostsWithFilters(categoryId, q, minPriceValue, maxPriceValue, pageable);

        model.addAttribute("posts", result.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalPosts", result.getTotalElements());

        // Check authentication status
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        return "marketplace";
    }

    // Search suggestions API
    @GetMapping("/api/search/suggestions")
    @ResponseBody
    public List<SearchSuggestion> searchSuggestions(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }
        List<Post> posts = categoryService.searchPosts(query.trim());
        return posts.stream()
                .limit(5)
                .map(post -> new SearchSuggestion(
                        post.getPostId(),
                        post.getTitle(),
                        post.getThumbnailUrl(),
                        post.getPrice() != null ? post.getPrice().doubleValue() : 0))
                .collect(Collectors.toList());
    }

    // Simple record for search suggestions
    public record SearchSuggestion(Integer id, String title, String thumbnailUrl, Double price) {}
}
