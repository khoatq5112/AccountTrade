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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        int currentPage = Math.max(page, 0);
        String keyword = q != null ? q.trim() : null;

        // Get all categories for sidebar
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        // Get current category
        Category currentCategory = null;
        if (categoryId != null) {
            currentCategory = categoryService.getCategoryById(categoryId);
        }
        model.addAttribute("currentCategory", currentCategory);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
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

        // Validate page size
        int pageSize = java.util.Arrays.asList(12, 24, 48).contains(size) ? size : 12;

        // Get paginated posts
        Pageable pageable = PageRequest.of(currentPage, pageSize, sortOption);
        var result = categoryService.getPostsWithFilters(categoryId, keyword, minPriceValue, maxPriceValue, pageable);

        model.addAttribute("posts", result.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalPosts", result.getTotalElements());

        // Calculate showing X to Y of Z
        long totalElements = result.getTotalElements();
        int fromItem = (int) (currentPage * pageSize + 1);
        int toItem = (int) Math.min((long) (currentPage + 1) * pageSize, totalElements);
        model.addAttribute("fromItem", totalElements > 0 ? fromItem : 0);
        model.addAttribute("toItem", toItem);

        // Pass price filter values for UI
        model.addAttribute("minPriceValue", minPriceValue != null ? minPriceValue.intValue() : 0);
        model.addAttribute("maxPriceValue", maxPriceValue != null ? maxPriceValue.intValue() : 10000000);

        addUserContext(model);

        return "marketplace";
    }

    @GetMapping("/marketplace/{postId}")
    public String marketplaceDetail(@PathVariable Integer postId, Model model) {
        Post post = categoryService.getPostById(postId);
        if (post == null) {
            return "redirect:/marketplace?error=not_found";
        }

        model.addAttribute("post", post);
        addUserContext(model);
        return "marketplace_detail";
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
                        post.getResolvedThumbnailUrl(),
                        post.getPrice() != null ? post.getPrice().doubleValue() : 0))
                .collect(Collectors.toList());
    }

    // Simple record for search suggestions
    public record SearchSuggestion(Integer id, String title, String thumbnailUrl, Double price) {}

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    private boolean canPurchase() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch("ROLE_SELLER"::equals);
    }

    private boolean isBuyer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_BUYER"::equals);
    }

    private void addUserContext(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        String currentRole = resolveCurrentRole(auth);

        model.addAttribute("isAuthenticated", authenticated);
        model.addAttribute("isBuyer", isBuyer());
        model.addAttribute("canPurchase", canPurchase());
        model.addAttribute("currentUsername", authenticated ? auth.getName() : null);
        model.addAttribute("currentRole", currentRole);
        model.addAttribute("accountDashboardUrl",
                "Seller".equals(currentRole) ? "/seller/dashboard" :
                ("Admin".equals(currentRole) ? "/admin/dashboard" : "/buyer/dashboard"));
    }

    private String resolveCurrentRole(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "";
        }

        if (auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_SELLER"::equals)) {
            return "Seller";
        }
        if (auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals)) {
            return "Admin";
        }
        if (auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_BUYER"::equals)) {
            return "Buyer";
        }
        return "";
    }
}
