package com.group3.accounttrade.service;

import com.group3.accounttrade.dto.CredentialForm;
import com.group3.accounttrade.dto.PostForm;
import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling post-related operations.
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final PostCredentialRepository postCredentialRepository;
    private final CredentialStatusRepository credentialStatusRepository;
    private final CredentialAssignmentRepository credentialAssignmentRepository;

    /**
     * Creates a new post from the form data.
     *
     * @param form     the post form data
     * @param username the username of the seller (authenticated user)
     * @return the created Post entity
     * @throws IOException              if image upload fails
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Post createPost(PostForm form, String username) throws IOException {
        // Get the seller (current authenticated user)
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        // Get category
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));

        // Upload thumbnail to Cloudinary if provided
        String thumbnailUrl = null;
        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            thumbnailUrl = cloudinaryService.uploadImage(form.getThumbnailFile());
        }

        // Determine initial stock status based on credentials
        boolean hasCredentials = form.hasCredentials();
        StockStatus stockStatus = hasCredentials ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK;

        // Create the post entity
        Post post = Post.builder()
                .seller(seller)
                .title(form.getTitle())
                .price(form.getPrice())
                .description(form.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .category(category)
                .stockStatus(stockStatus)
                .credentials(new ArrayList<>())
                .build();

        // Save post first
        post = postRepository.save(post);

        // Get "Available" status for credentials
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trạng thái 'Available'"));

        // Create and save credentials if provided
        if (hasCredentials) {
            for (CredentialForm credForm : form.getCredentials()) {
                if (credForm != null && credForm.hasData()) {
                    PostCredential credential = PostCredential.builder()
                            .post(post)
                            .accountUsername(credForm.getAccountUsername())
                            .accountPassword(credForm.getAccountPassword())
                            .securityNotes(credForm.getSecurityNotes())
                            .credentialStatus(availableStatus)
                            .build();
                    postCredentialRepository.save(credential);
                }
            }
        }

        return post;
    }

    /**
     * Gets all posts for a seller with pagination.
     *
     * @param username the seller's username
     * @param pageable pagination information
     * @return a page of posts
     */
    public Page<Post> getSellerPosts(String username, Pageable pageable) {
        Page<Post> page = postRepository.findBySeller_UsernameOrderByCreatedAtDesc(username, pageable);
        synchronizePostStockStatuses(page.getContent());
        return page;
    }

    /**
     * Gets seller posts with filters.
     *
     * @param username    the seller's username
     * @param keyword     search keyword (can be null)
     * @param categoryId  category filter (can be null)
     * @param stockStatus stock status filter (can be null)
     * @param pageable    pagination information
     * @return a page of filtered posts
     */
    public Page<Post> getSellerPostsWithFilters(String username, String keyword, Integer categoryId,
                                                 StockStatus stockStatus, Pageable pageable) {
        Page<Post> page = postRepository.findSellerPostsWithFilters(username, keyword, categoryId, stockStatus, pageable);
        synchronizePostStockStatuses(page.getContent());
        return page;
    }

    /**
     * Gets a post by ID for a specific seller.
     *
     * @param postId   the post ID
     * @param username the seller's username
     * @return the post if found
     * @throws IllegalArgumentException if post not found or doesn't belong to seller
     */
    public Post getPostByIdAndSeller(Integer postId, String username) {
        Post post = postRepository.findByPostIdAndSeller_Username(postId, username)
                .orElseThrow(() -> new IllegalArgumentException("Bài đăng không tồn tại hoặc bạn không có quyền truy cập"));
        synchronizePostStockStatus(post);
        return post;
    }

    /**
     * Gets a post by ID (public access - no credential data).
     *
     * @param postId the post ID
     * @return the post if found
     * @throws IllegalArgumentException if post not found
     */
    public Post getPostById(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Bài đăng không tồn tại"));
        synchronizePostStockStatus(post);
        return post;
    }

    /**
     * Gets all credentials for a post (for seller management).
     *
     * @param postId the post ID
     * @return list of all credentials for the post
     */
    public List<PostCredential> getCredentialsByPostId(Integer postId) {
        List<PostCredential> credentials = postCredentialRepository.findAllForManagementByPostId(postId);
        populateSoldOrderNumbers(credentials);
        return credentials;
    }

    /**
     * Gets credentials for a post with pagination, filtering, and sorting.
     *
     * @param postId     the post ID
     * @param statusName the status name filter (can be null)
     * @param keyword    the search keyword for username (can be null)
     * @param pageable   pagination and sorting information
     * @return a page of credentials
     */
    @Transactional(readOnly = true)
    public Page<PostCredential> getCredentialsByPostIdWithFilters(
            Integer postId, String statusName, String keyword, Pageable pageable) {
        Page<PostCredential> page = postCredentialRepository.findByPostIdWithFilters(postId, statusName, keyword, pageable);
        populateSoldOrderNumbers(page.getContent());
        return page;
    }

    /**
     * Updates an existing post (basic info only - not credentials).
     *
     * @param postId   the post ID to update
     * @param form     the updated form data
     * @param username the seller's username
     * @return the updated post
     * @throws IOException              if image upload fails
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public Post updatePost(Integer postId, PostForm form, String username) throws IOException {
        Post post = getPostByIdAndSeller(postId, username);

        // Update category
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        post.setCategory(category);

        // Update basic fields
        post.setTitle(form.getTitle());
        post.setPrice(form.getPrice());
        post.setDescription(form.getDescription());

        // Update thumbnail if new one provided
        if (form.getThumbnailFile() != null && !form.getThumbnailFile().isEmpty()) {
            String thumbnailUrl = cloudinaryService.uploadImage(form.getThumbnailFile());
            post.setThumbnailUrl(thumbnailUrl);
        }

        return postRepository.save(post);
    }

    /**
     * Deletes a post by ID.
     *
     * @param postId   the post ID to delete
     * @param username the seller's username
     * @throws IllegalArgumentException if post not found or doesn't belong to seller
     */
    @Transactional
    public void deletePost(Integer postId, String username) {
        Post post = getPostByIdAndSeller(postId, username);
        // Credentials will be deleted automatically due to cascade
        postRepository.delete(post);
    }

    // ==================== Credential Management ====================

    /**
     * Adds a single credential to a post.
     *
     * @param postId   the post ID
     * @param form     the credential form
     * @param username the seller's username
     * @return the created credential
     */
    @Transactional
    public PostCredential addCredential(Integer postId, CredentialForm form, String username) {
        Post post = getPostByIdAndSeller(postId, username);

        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trạng thái 'Available'"));

        PostCredential credential = PostCredential.builder()
                .post(post)
                .accountUsername(form.getAccountUsername())
                .accountPassword(form.getAccountPassword())
                .securityNotes(form.getSecurityNotes())
                .credentialStatus(availableStatus)
                .build();

        PostCredential saved = postCredentialRepository.save(credential);

        // Update stock status if this is the first available credential
        updateStockStatus(postId);

        return saved;
    }

    /**
     * Adds multiple credentials to a post at once.
     *
     * @param postId   the post ID
     * @param forms    list of credential forms
     * @param username the seller's username
     * @return list of created credentials
     */
    @Transactional
    public List<PostCredential> addCredentials(Integer postId, List<CredentialForm> forms, String username) {
        Post post = getPostByIdAndSeller(postId, username);

        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trạng thái 'Available'"));

        List<PostCredential> credentials = new ArrayList<>();
        for (CredentialForm form : forms) {
            if (form != null && form.hasData()) {
                credentials.add(PostCredential.builder()
                        .post(post)
                        .accountUsername(form.getAccountUsername())
                        .accountPassword(form.getAccountPassword())
                        .securityNotes(form.getSecurityNotes())
                        .credentialStatus(availableStatus)
                        .build());
            }
        }

        List<PostCredential> saved = postCredentialRepository.saveAll(credentials);

        // Update stock status
        updateStockStatus(postId);

        return saved;
    }

    /**
     * Updates an existing credential.
     *
     * @param credentialId the credential ID
     * @param form         the updated credential form
     * @param username     the seller's username
     * @return the updated credential
     */
    @Transactional
    public PostCredential updateCredential(Integer credentialId, CredentialForm form, String username) {
        PostCredential credential = postCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        // Verify ownership
        if (!credential.getPost().getSeller().getUsername().equals(username)) {
            throw new IllegalArgumentException("You do not have permission to edit this credential");
        }

        // Only allow editing if credential is still available
        if (credential.isSold()) {
            throw new IllegalArgumentException("Cannot edit a sold credential");
        }

        credential.setAccountUsername(form.getAccountUsername());
        credential.setAccountPassword(form.getAccountPassword());
        credential.setSecurityNotes(form.getSecurityNotes());

        return postCredentialRepository.save(credential);
    }

    /**
     * Deletes a credential (only if not sold).
     *
     * @param credentialId the credential ID
     * @param username     the seller's username
     */
    @Transactional
    public void deleteCredential(Integer credentialId, String username) {
        PostCredential credential = postCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        // Verify ownership
        if (!credential.getPost().getSeller().getUsername().equals(username)) {
            throw new IllegalArgumentException("You do not have permission to delete this credential");
        }

        // Only allow deletion if credential is still available
        if (credential.isSold()) {
            throw new IllegalArgumentException("Cannot delete a sold credential");
        }

        Integer postId = credential.getPost().getPostId();
        postCredentialRepository.delete(credential);

        // Update stock status
        updateStockStatus(postId);
    }

    /**
     * Updates the stock status of a post based on available credentials.
     *
     * @param postId the post ID
     */
    @Transactional
    public void updateStockStatus(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElse(null);

        boolean hasAvailable = availableStatus != null && 
                postCredentialRepository.existsByPost_PostIdAndCredentialStatus(postId, availableStatus);

        StockStatus newStatus = hasAvailable ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK;

        if (post.getStockStatus() != newStatus) {
            post.setStockStatus(newStatus);
            postRepository.save(post);
        }
    }

    /**
     * Calculates whether a post can currently be purchased.
     * A post must be marked as in stock and still have at least one available credential.
     *
     * @param post the post to evaluate
     * @return purchase availability result
     */
    public PurchaseAvailability getPurchaseAvailability(Post post) {
        if (post == null) {
            return new PurchaseAvailability(false, "Sản phẩm không tồn tại.", 0);
        }

        long availableCredentials = post.getPostId() != null
                ? getCredentialStats(post.getPostId()).available()
                : post.getAvailableCredentialCount();

        if (!post.isInStock()) {
            return new PurchaseAvailability(false, "Sản phẩm hiện chưa sẵn sàng để mua.", availableCredentials);
        }

        if (availableCredentials <= 0) {
            return new PurchaseAvailability(false, "Sản phẩm đã hết tài khoản khả dụng.", 0);
        }

        return new PurchaseAvailability(true, null, availableCredentials);
    }

    /**
     * Gets credential statistics for a post.
     *
     * @param postId the post ID
     * @return CredentialStats object
     */
    public CredentialStats getCredentialStats(Integer postId) {
        long total = postCredentialRepository.countByPost_PostId(postId);
        
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElse(null);
        CredentialStatus holdingStatus = credentialStatusRepository.findByStatusName(CredentialStatus.HOLDING)
                .orElse(null);
        CredentialStatus soldStatus = credentialStatusRepository.findByStatusName(CredentialStatus.SOLD)
                .orElse(null);
        
        long available = availableStatus != null 
                ? postCredentialRepository.countByPost_PostIdAndCredentialStatus(postId, availableStatus) 
                : 0;
        long holding = holdingStatus != null
                ? postCredentialRepository.countByPost_PostIdAndCredentialStatus(postId, holdingStatus)
                : 0;
        long sold = (soldStatus != null
                ? postCredentialRepository.countByPost_PostIdAndCredentialStatus(postId, soldStatus) 
                : 0) + holding;

        return new CredentialStats(total, available, sold);
    }

    @Transactional
    protected void synchronizePostStockStatuses(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        for (Post post : posts) {
            synchronizePostStockStatus(post);
        }
    }

    @Transactional
    protected void synchronizePostStockStatus(Post post) {
        if (post == null || post.getPostId() == null) {
            return;
        }

        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElse(null);

        boolean hasAvailable = availableStatus != null
                && postCredentialRepository.existsByPost_PostIdAndCredentialStatus(post.getPostId(), availableStatus);
        StockStatus actualStatus = hasAvailable ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK;

        if (post.getStockStatus() != actualStatus) {
            post.setStockStatus(actualStatus);
            postRepository.save(post);
        }
    }

    /**
     * Checks if a post has available credentials.
     *
     * @param postId the post ID
     * @return true if at least one available credential exists
     */
    public boolean hasAvailableCredentials(Integer postId) {
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElse(null);
        if (availableStatus == null) return false;
        return postCredentialRepository.existsByPost_PostIdAndCredentialStatus(postId, availableStatus);
    }

    /**
     * Counts available credentials for a post.
     *
     * @param postId the post ID
     * @return count of available credentials
     */
    public long countAvailableCredentials(Integer postId) {
        CredentialStatus availableStatus = credentialStatusRepository.findByStatusName(CredentialStatus.AVAILABLE)
                .orElse(null);
        if (availableStatus == null) return 0;
        return postCredentialRepository.countByPost_PostIdAndCredentialStatus(postId, availableStatus);
    }

    private void populateSoldOrderNumbers(List<PostCredential> credentials) {
        for (PostCredential credential : credentials) {
            credential.setSoldOrderNumber(
                    credentialAssignmentRepository.findByCredentialOrderByAssignedAtDesc(credential).stream()
                            .map(assignment -> assignment.getOrderItem())
                            .filter(orderItem -> orderItem != null && orderItem.getOrder() != null)
                            .map(orderItem -> orderItem.getOrder().getOrderNumber())
                            .findFirst()
                            .orElse(null)
            );
        }
    }

    // ==================== Inner Classes ====================

    /**
     * DTO for credential statistics.
     */
    public record CredentialStats(long total, long available, long sold) {
        public boolean isInStock() {
            return available > 0;
        }
    }

    /**
     * DTO describing whether a post can be purchased right now.
     */
    public record PurchaseAvailability(boolean purchasable, String failureReason, long availableCredentialCount) {
    }
}
