package com.group3.accounttrade.controller;

import com.group3.accounttrade.dto.CredentialForm;
import com.group3.accounttrade.dto.PostForm;
import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.CategoryRepository;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.repository.WalletRepository;
import com.group3.accounttrade.service.PostService;
import com.group3.accounttrade.service.SellerDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * Controller for seller-related operations.
 */
@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerController {

    private final CategoryRepository categoryRepository;
    private final PostService postService;
    private final SellerDashboardService sellerDashboardService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @GetMapping("/dashboard")
    public String viewSellerDashboard(Authentication authentication, Model model) {
        String username = authentication.getName();

        // Get dashboard statistics
        SellerDashboardService.SellerDashboardStats stats = sellerDashboardService.getDashboardStats(username);
        model.addAttribute("stats", stats);

        // Get pending orders requiring action
        List<Transaction> pendingOrders = sellerDashboardService.getPendingOrders(username, 5);
        model.addAttribute("pendingOrders", pendingOrders);

        // Get user info
        User user = userRepository.findByUsername(username).orElse(null);
        model.addAttribute("currentUser", user);

        // Get wallet
        Wallet wallet = walletRepository.findByUser_Username(username).orElse(null);
        model.addAttribute("wallet", wallet);
        addSellerLayoutContext(model, user, "dashboard");

        return "seller_dashboard";
    }

    /**
     * Display the form for creating a new post.
     */
    @GetMapping("/posts/new")
    public String showCreatePostForm(Model model) {
        model.addAttribute("postForm", new PostForm());

        // Get all categories as a simple list
        List<Category> categories = categoryRepository.findAllOrderByDisplayOrderAsc();
        model.addAttribute("categories", categories);

        return "seller_create_post";
    }

    /**
     * Handle the submission of the post creation form.
     */
    @PostMapping("/posts")
    public String createPost(
            @Valid @ModelAttribute("postForm") PostForm postForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        // If validation errors exist, return to the form
        if (bindingResult.hasErrors()) {
            addCategoriesToModel(model);
            return "seller_create_post";
        }

        try {
            // Get the authenticated username
            String username = authentication.getName();

            // Create the post
            postService.createPost(postForm, username);

            // Add success message and redirect to dashboard
            redirectAttributes.addFlashAttribute("successMessage", "Đăng bài thành công! Bài đăng của bạn đã được tạo.");
            return "redirect:/seller/posts";

        } catch (IOException e) {
            // Handle image upload error
            model.addAttribute("errorMessage", "Lỗi tải lên hình ảnh: " + e.getMessage());
            addCategoriesToModel(model);
            return "seller_create_post";

        } catch (IllegalArgumentException e) {
            // Handle validation errors from service
            model.addAttribute("errorMessage", e.getMessage());
            addCategoriesToModel(model);
            return "seller_create_post";

        } catch (Exception e) {
            // Handle unexpected errors
            model.addAttribute("errorMessage", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.");
            addCategoriesToModel(model);
            return "seller_create_post";
        }
    }

    /**
     * Display the list of seller's posts with search, filter, sorting, and pagination.
     */
    @GetMapping("/posts")
    public String listSellerPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();
        
        // Validate page size (only allow 10, 25, 50)
        int pageSize = (size == 25 || size == 50) ? size : 10;
        
        // Create sort order
        org.springframework.data.domain.Sort.Direction sortDirection =
            "asc".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
        org.springframework.data.domain.Sort sortOrder = org.springframework.data.domain.Sort.by(sortDirection, sort);
        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);
        
        // Convert stockStatus string to enum
        StockStatus stockStatusEnum = null;
        if (stockStatus != null && !stockStatus.isEmpty()) {
            try {
                stockStatusEnum = StockStatus.valueOf(stockStatus);
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }

        Page<Post> postsPage = postService.getSellerPostsWithFilters(
            username, keyword, categoryId, stockStatusEnum, pageable);

        model.addAttribute("postsPage", postsPage);
        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalItems", postsPage.getTotalElements());
        
        // Calculate showing X to Y of Z
        long fromItem = postsPage.getTotalElements() > 0 ? (long) page * pageSize + 1 : 0;
        long toItem = Math.min((long) (page + 1) * pageSize, postsPage.getTotalElements());
        model.addAttribute("fromItem", fromItem);
        model.addAttribute("toItem", toItem);
        
        // Sorting attributes
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);

        // Add filter parameters to model
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStockStatus", stockStatus);

        // Add categories for filter dropdowns
        model.addAttribute("categories", categoryRepository.findAllOrderByDisplayOrderAsc());
        addSellerLayoutContext(model, userRepository.findByUsername(username).orElse(null), "posts");

        return "seller_posts";
    }

    /**
     * Display the form for editing an existing post.
     */
    @GetMapping("/posts/{postId}/edit")
    public String showEditPostForm(
            @PathVariable Integer postId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            String username = authentication.getName();
            Post post = postService.getPostByIdAndSeller(postId, username);

            // Create form from existing post
            PostForm postForm = new PostForm();
            postForm.setTitle(post.getTitle());
            postForm.setPrice(post.getPrice());
            postForm.setDescription(post.getDescription());
            postForm.setCategoryId(post.getCategory().getCategoryId());

            model.addAttribute("postForm", postForm);
            model.addAttribute("post", post);
            model.addAttribute("categories", categoryRepository.findAllOrderByDisplayOrderAsc());

            // Add credential statistics
            PostService.CredentialStats stats = postService.getCredentialStats(postId);
            model.addAttribute("credentialStats", stats);

            return "seller_edit_post";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/posts";
        }
    }

    /**
     * Handle the submission of the post edit form.
     */
    @PostMapping("/posts/{postId}")
    public String updatePost(
            @PathVariable Integer postId,
            @Valid @ModelAttribute("postForm") PostForm postForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            try {
                String username = authentication.getName();
                Post post = postService.getPostByIdAndSeller(postId, username);
                model.addAttribute("post", post);
                model.addAttribute("categories", categoryRepository.findAllOrderByDisplayOrderAsc());
                model.addAttribute("credentialStats", postService.getCredentialStats(postId));
                return "seller_edit_post";
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/seller/posts";
            }
        }

        try {
            String username = authentication.getName();
            postService.updatePost(postId, postForm, username);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bài đăng thành công!");
            return "redirect:/seller/posts";

        } catch (IOException e) {
            model.addAttribute("errorMessage", "Lỗi tải lên hình ảnh: " + e.getMessage());
            addCategoriesToModelForEdit(model, postId, authentication);
            return "seller_edit_post";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addCategoriesToModelForEdit(model, postId, authentication);
            return "seller_edit_post";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.");
            addCategoriesToModelForEdit(model, postId, authentication);
            return "seller_edit_post";
        }
    }

    /**
     * Handle post deletion.
     */
    @PostMapping("/posts/{postId}/delete")
    public String deletePost(
            @PathVariable Integer postId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String username = authentication.getName();
            postService.deletePost(postId, username);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa bài đăng thành công!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xóa bài đăng.");
        }

        return "redirect:/seller/posts";
    }

    // ==================== Credential Management Endpoints ====================

    /**
     * Display the credential management page for a post with pagination, filtering, and sorting.
     */
    @GetMapping("/posts/{postId}/credentials")
    public String manageCredentials(
            @PathVariable Integer postId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            String username = authentication.getName();
            Post post = postService.getPostByIdAndSeller(postId, username);

            // Validate page size (only allow 10, 25, 50)
            int pageSize = (size == 25 || size == 50) ? size : 10;

            // Create sort order
            org.springframework.data.domain.Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                    ? org.springframework.data.domain.Sort.Direction.ASC
                    : org.springframework.data.domain.Sort.Direction.DESC;
            org.springframework.data.domain.Sort sortOrder = org.springframework.data.domain.Sort.by(sortDirection, sort);
            Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

            // Get paginated credentials
            Page<PostCredential> credentialsPage = postService.getCredentialsByPostIdWithFilters(
                postId, status, keyword, pageable);

            // Get stats (still need full stats for the summary cards)
            PostService.CredentialStats stats = postService.getCredentialStats(postId);

            model.addAttribute("post", post);
            model.addAttribute("credentialsPage", credentialsPage);
            model.addAttribute("credentials", credentialsPage.getContent());
            model.addAttribute("credentialStats", stats);
            model.addAttribute("credentialForm", new CredentialForm());

            // Pagination attributes
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("totalPages", credentialsPage.getTotalPages());
            model.addAttribute("totalItems", credentialsPage.getTotalElements());

            // Calculate showing X to Y of Z
            long fromItem = credentialsPage.getTotalElements() > 0 ? (long) page * pageSize + 1 : 0;
            long toItem = Math.min((long) (page + 1) * pageSize, credentialsPage.getTotalElements());
            model.addAttribute("fromItem", fromItem);
            model.addAttribute("toItem", toItem);

            // Sorting attributes
            model.addAttribute("currentSort", sort);
            model.addAttribute("currentDirection", direction);

            // Filter parameters
            model.addAttribute("keyword", keyword);
            model.addAttribute("selectedStatus", status);

            return "seller_post_credentials";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/seller/posts";
        }
    }

    /**
     * Add a single credential to a post.
     */
    @PostMapping("/posts/{postId}/credentials")
    public String addCredential(
            @PathVariable Integer postId,
            @Valid @ModelAttribute("credentialForm") CredentialForm credentialForm,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng điền đầy đủ thông tin tài khoản.");
            return "redirect:/seller/posts/" + postId + "/credentials";
        }

        try {
            String username = authentication.getName();
            postService.addCredential(postId, credentialForm, username);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm tài khoản thành công!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi thêm tài khoản.");
        }

        return "redirect:/seller/posts/" + postId + "/credentials";
    }

    /**
     * Add multiple credentials to a post (bulk add).
     */
    @PostMapping("/posts/{postId}/credentials/bulk")
    @ResponseBody
    public ResponseEntity<?> addCredentialsBulk(
            @PathVariable Integer postId,
            @RequestBody List<CredentialForm> credentialForms,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            List<PostCredential> added = postService.addCredentials(postId, credentialForms, username);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Đã thêm " + added.size() + " tài khoản thành công!",
                    "count", added.size()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                    "success", false,
                    "message", "Đã xảy ra lỗi khi thêm tài khoản."
            ));
        }
    }

    /**
     * Update an existing credential (AJAX).
     */
    @PostMapping("/credentials/{credentialId}")
    @ResponseBody
    public ResponseEntity<?> updateCredential(
            @PathVariable Integer credentialId,
            @Valid @RequestBody CredentialForm credentialForm,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            PostCredential updated = postService.updateCredential(credentialId, credentialForm, username);
            return ResponseEntity.ok().body(java.util.Map.of(
                    "success", true,
                    "message", "Cập nhật thành công",
                    "credentialId", updated.getCredentialId()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                    "success", false,
                    "message", "Đã xảy ra lỗi"
            ));
        }
    }

    /**
     * Delete a credential (only if not sold).
     */
    @PostMapping("/credentials/{credentialId}/delete")
    public String deleteCredential(
            @PathVariable Integer credentialId,
            @RequestParam Integer postId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String username = authentication.getName();
            postService.deleteCredential(credentialId, username);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xóa tài khoản.");
        }

        return "redirect:/seller/posts/" + postId + "/credentials";
    }

    /**
     * Get credential statistics for a post (AJAX).
     */
    @GetMapping("/posts/{postId}/credentials/stats")
    @ResponseBody
    public ResponseEntity<PostService.CredentialStats> getCredentialStats(
            @PathVariable Integer postId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            // Verify ownership
            postService.getPostByIdAndSeller(postId, username);
            
            PostService.CredentialStats stats = postService.getCredentialStats(postId);
            return ResponseEntity.ok(stats);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to add categories to the model.
     */
    private void addCategoriesToModel(Model model) {
        List<Category> categories = categoryRepository.findAllOrderByDisplayOrderAsc();
        model.addAttribute("categories", categories);
    }

    /**
     * Helper method to add categories to model for edit form.
     */
    private void addCategoriesToModelForEdit(Model model, Integer postId, Authentication authentication) {
        try {
            Post post = postService.getPostByIdAndSeller(postId, authentication.getName());
            model.addAttribute("post", post);
            model.addAttribute("credentialStats", postService.getCredentialStats(postId));
        } catch (Exception ignored) {
            // Ignore if post not found
        }
        model.addAttribute("categories", categoryRepository.findAllOrderByDisplayOrderAsc());
    }

    private void addSellerLayoutContext(Model model, User user, String activeSellerNav) {
        String sellerUsername = user != null ? user.getUsername() : null;
        model.addAttribute("currentUser", user);
        model.addAttribute("sellerUsername", sellerUsername);
        model.addAttribute("sellerBadgeText", "Đã xác minh cấp 2");
        model.addAttribute("activeSellerNav", activeSellerNav);
    }
}
