# Pagination, Search Filtering, and Sorting Implementation Plan

## Overview

This plan outlines the implementation of pagination, search filtering, and sorting functionality for two key pages:
1. **Quản lý bài đăng** (Post Management) - `/seller/posts`
2. **Quản lý tài khoản** (Credential Management) - `/seller/posts/{postId}/credentials`

## Current State Analysis

### Post Management (`/seller/posts`)
- ✅ Basic pagination exists (fixed page size of 10)
- ✅ Search by title keyword exists
- ✅ Filter by category exists
- ⚠️ Stock status filter parameter exists but is NOT used in the query
- ❌ No sorting functionality
- ❌ No configurable page size
- ❌ No results summary

### Credential Management (`/seller/posts/{postId}/credentials`)
- ❌ No pagination (loads ALL credentials)
- ❌ No search functionality
- ❌ No filter by status
- ❌ No sorting
- ❌ No configurable page size

---

## Implementation Plan

### Phase 1: Post Management Enhancements

#### 1.1 Update PostRepository

Add sorting support and fix stock status filter:

```java
// In PostRepository.java
@Query("""
    SELECT p FROM Post p
    JOIN p.seller s
    WHERE s.username = :username
    AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
    AND (:stockStatus IS NULL OR p.stockStatus = :stockStatus)
    """)
Page<Post> findBySellerWithFilters(
    @Param("username") String username,
    @Param("keyword") String keyword,
    @Param("categoryId") Integer categoryId,
    @Param("stockStatus") StockStatus stockStatus,
    Pageable pageable
);
```

#### 1.2 Update SellerController.listSellerPosts

```java
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
    
    // Validate page size
    int pageSize = Arrays.asList(10, 25, 50).contains(size) ? size : 10;
    
    // Create sort
    Sort sortOrder = Sort.by(direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort);
    Pageable pageable = PageRequest.of(page, pageSize, sortOrder);
    
    // Convert stockStatus string to enum
    StockStatus stockStatusEnum = (stockStatus != null && !stockStatus.isEmpty()) 
        ? StockStatus.valueOf(stockStatus) : null;
    
    Page<Post> postsPage = postService.getSellerPostsWithFilters(
        username, keyword, categoryId, stockStatusEnum, pageable
    );

    model.addAttribute("postsPage", postsPage);
    model.addAttribute("posts", postsPage.getContent());
    model.addAttribute("currentPage", page);
    model.addAttribute("pageSize", pageSize);
    model.addAttribute("totalPages", postsPage.getTotalPages());
    model.addAttribute("totalItems", postsPage.getTotalElements());
    
    // Calculate showing X to Y of Z
    int fromItem = (int) (page * pageSize + 1);
    int toItem = (int) Math.min((page + 1) * pageSize, postsPage.getTotalElements());
    model.addAttribute("fromItem", postsPage.getTotalElements() > 0 ? fromItem : 0);
    model.addAttribute("toItem", toItem);
    
    // Sorting attributes
    model.addAttribute("currentSort", sort);
    model.addAttribute("currentDirection", direction);
    
    // Filter parameters
    model.addAttribute("keyword", keyword);
    model.addAttribute("selectedCategoryId", categoryId);
    model.addAttribute("selectedStockStatus", stockStatus);
    
    model.addAttribute("categories", categoryRepository.findAllOrderByDisplayOrderAsc());

    return "seller_posts";
}
```

#### 1.3 Update seller_posts.html Template

Add sortable column headers:

```html
<thead>
    <tr class="border-b-2 border-gray-100 text-xs uppercase tracking-wider text-gray-500">
        <th class="py-3 px-4 font-bold">
            <a th:href="@{/seller/posts(page=0, keyword=${keyword}, categoryId=${selectedCategoryId}, stockStatus=${selectedStockStatus}, size=${pageSize}, sort='postId', direction=${currentSort == 'postId' and currentDirection == 'desc' ? 'asc' : 'desc'})}"
               class="hover:text-primary flex items-center gap-1">
                ID
                <i th:if="${currentSort == 'postId'}" class="ph ph-bold" th:classappend="${currentDirection == 'asc'} ? 'ph-caret-up' : 'ph-caret-down'"></i>
            </a>
        </th>
        <th class="py-3 px-4 font-bold">Hình ảnh</th>
        <th class="py-3 px-4 font-bold">
            <a th:href="@{/seller/posts(...sort='title'...)}" class="hover:text-primary flex items-center gap-1">
                Tiêu đề
                <i th:if="${currentSort == 'title'}" ...></i>
            </a>
        </th>
        <th class="py-3 px-4 font-bold">Danh mục</th>
        <th class="py-3 px-4 font-bold">
            <a th:href="@{/seller/posts(...sort='price'...)}" class="hover:text-primary flex items-center gap-1">
                Giá
                <i th:if="${currentSort == 'price'}" ...></i>
            </a>
        </th>
        <th class="py-3 px-4 font-bold">
            <a th:href="@{/seller/posts(...sort='stockStatus'...)}" class="hover:text-primary flex items-center gap-1">
                Trạng thái
                <i th:if="${currentSort == 'stockStatus'}" ...></i>
            </a>
        </th>
        <th class="py-3 px-4 font-bold">
            <a th:href="@{/seller/posts(...sort='createdAt'...)}" class="hover:text-primary flex items-center gap-1">
                Ngày tạo
                <i th:if="${currentSort == 'createdAt'}" ...></i>
            </a>
        </th>
        <th class="py-3 px-4 font-bold text-right">Thao tác</th>
    </tr>
</thead>
```

Add page size selector and results summary:

```html
<!-- Results Summary and Page Size -->
<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-4">
    <p class="text-sm text-gray-500">
        Hiển thị <span class="font-bold" th:text="${fromItem}">1</span>
        đến <span class="font-bold" th:text="${toItem}">10</span>
        trong tổng số <span class="font-bold" th:text="${totalItems}">0</span> kết quả
    </p>
    <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">Hiển thị:</span>
        <select id="pageSize" onchange="changePageSize(this.value)" 
                class="px-3 py-1 border border-gray-200 rounded-lg text-sm">
            <option value="10" th:selected="${pageSize == 10}">10</option>
            <option value="25" th:selected="${pageSize == 25}">25</option>
            <option value="50" th:selected="${pageSize == 50}">50</option>
        </select>
        <span class="text-sm text-gray-500">mỗi trang</span>
    </div>
</div>

<script>
function changePageSize(size) {
    const url = new URL(window.location);
    url.searchParams.set('size', size);
    url.searchParams.set('page', '0'); // Reset to first page
    window.location.href = url.toString();
}
</script>
```

---

### Phase 2: Credential Management Pagination

#### 2.1 Update PostCredentialRepository

Add pagination and filtering support:

```java
// In PostCredentialRepository.java
@Query("""
    SELECT pc FROM PostCredential pc
    JOIN FETCH pc.credentialStatus cs
    LEFT JOIN FETCH pc.soldToOrder sto
    WHERE pc.post.postId = :postId
    AND (:statusName IS NULL OR cs.statusName = :statusName)
    AND (:keyword IS NULL OR LOWER(pc.accountUsername) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY 
        CASE WHEN :sortDirection = 'desc' THEN pc.createdAt END DESC,
        CASE WHEN :sortDirection = 'asc' THEN pc.createdAt END ASC
    """)
Page<PostCredential> findByPostIdWithFilters(
    @Param("postId") Integer postId,
    @Param("statusName") String statusName,
    @Param("keyword") String keyword,
    @Param("sortDirection") String sortDirection,
    Pageable pageable
);

// Count for pagination
@Query("""
    SELECT COUNT(pc) FROM PostCredential pc
    JOIN pc.credentialStatus cs
    WHERE pc.post.postId = :postId
    AND (:statusName IS NULL OR cs.statusName = :statusName)
    AND (:keyword IS NULL OR LOWER(pc.accountUsername) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
long countByPostIdWithFilters(
    @Param("postId") Integer postId,
    @Param("statusName") String statusName,
    @Param("keyword") String keyword
);
```

#### 2.2 Update SellerController.manageCredentials

```java
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
        
        // Validate page size
        int pageSize = Arrays.asList(10, 25, 50).contains(size) ? size : 10;
        
        // Create pageable
        Sort sortOrder = Sort.by(direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort);
        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);
        
        // Get paginated credentials
        Page<PostCredential> credentialsPage = postService.getCredentialsByPostIdWithFilters(
            postId, status, keyword, pageable
        );
        
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
        int fromItem = (int) (page * pageSize + 1);
        int toItem = (int) Math.min((page + 1) * pageSize, credentialsPage.getTotalElements());
        model.addAttribute("fromItem", credentialsPage.getTotalElements() > 0 ? fromItem : 0);
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
```

#### 2.3 Update PostService

Add method for paginated credentials:

```java
// In PostService.java
@Transactional(readOnly = true)
public Page<PostCredential> getCredentialsByPostIdWithFilters(
        Integer postId, String status, String keyword, Pageable pageable) {
    return postCredentialRepository.findByPostIdWithFilters(postId, status, keyword, 
        pageable.getSort().getOrderFor("createdAt").getDirection().name(), pageable);
}
```

#### 2.4 Update seller_post_credentials.html Template

Add search/filter form:

```html
<!-- Filters Card -->
<div class="card mb-6">
    <form th:action="@{/seller/posts/{id}/credentials(id=${post.postId})}" method="GET" 
          class="flex flex-col md:flex-row gap-4">
        <!-- Search -->
        <div class="flex-1">
            <label class="block text-sm font-bold text-gray-600 mb-1">Tìm kiếm</label>
            <div class="relative">
                <i class="ph ph-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></i>
                <input type="text" name="keyword" th:value="${keyword}"
                       placeholder="Tìm theo username..."
                       class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary">
            </div>
        </div>
        
        <!-- Status Filter -->
        <div class="w-full md:w-48">
            <label class="block text-sm font-bold text-gray-600 mb-1">Trạng thái</label>
            <select name="status" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary">
                <option value="">Tất cả</option>
                <option value="Available" th:selected="${selectedStatus == 'Available'}">Có sẵn</option>
                <option value="Sold" th:selected="${selectedStatus == 'Sold'}">Đã bán</option>
                <option value="Holding" th:selected="${selectedStatus == 'Holding'}">Đang giữ</option>
                <option value="Hidden" th:selected="${selectedStatus == 'Hidden'}">Đã ẩn</option>
            </select>
        </div>
        
        <!-- Filter Buttons -->
        <div class="flex items-end gap-2">
            <button type="submit" class="btn-primary flex items-center gap-2">
                <i class="ph-bold ph-funnel"></i> Lọc
            </button>
            <a th:href="@{/seller/posts/{id}/credentials(id=${post.postId})}" class="btn-outline">
                <i class="ph-bold ph-arrow-counter-clockwise"></i>
            </a>
        </div>
    </form>
</div>
```

Add results summary and page size selector:

```html
<!-- Results Summary -->
<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-4">
    <p class="text-sm text-gray-500">
        Hiển thị <span class="font-bold" th:text="${fromItem}">1</span>
        đến <span class="font-bold" th:text="${toItem}">10</span>
        trong tổng số <span class="font-bold" th:text="${totalItems}">0</span> tài khoản
    </p>
    <div class="flex items-center gap-2">
        <span class="text-sm text-gray-500">Hiển thị:</span>
        <select id="pageSize" onchange="changePageSize(this.value)" 
                class="px-3 py-1 border border-gray-200 rounded-lg text-sm">
            <option value="10" th:selected="${pageSize == 10}">10</option>
            <option value="25" th:selected="${pageSize == 25}">25</option>
            <option value="50" th:selected="${pageSize == 50}">50</option>
        </select>
        <span class="text-sm text-gray-500">mỗi trang</span>
    </div>
</div>
```

Add pagination controls:

```html
<!-- Pagination -->
<div th:if="${totalPages > 1}" class="flex justify-center items-center gap-2 mt-6 pt-6 border-t border-gray-100">
    <!-- Previous -->
    <a th:if="${currentPage > 0}"
       th:href="@{/seller/posts/{id}/credentials(id=${post.postId}, page=${currentPage - 1}, size=${pageSize}, keyword=${keyword}, status=${selectedStatus}, sort=${currentSort}, direction=${currentDirection})}"
       class="px-3 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
        <i class="ph-bold ph-caret-left"></i>
    </a>
    <span th:if="${currentPage == 0}" class="px-3 py-2 text-gray-300 cursor-not-allowed">
        <i class="ph-bold ph-caret-left"></i>
    </span>

    <!-- Page Numbers -->
    <th:block th:each="i : ${#numbers.sequence(0, totalPages - 1)}">
        <a th:if="${i != currentPage}"
           th:href="@{/seller/posts/{id}/credentials(id=${post.postId}, page=${i}, size=${pageSize}, keyword=${keyword}, status=${selectedStatus}, sort=${currentSort}, direction=${currentDirection})}"
           th:text="${i + 1}"
           class="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors font-bold">
        </a>
        <span th:if="${i == currentPage}"
              th:text="${i + 1}"
              class="px-4 py-2 bg-primary text-white rounded-lg font-bold">
        </span>
    </th:block>

    <!-- Next -->
    <a th:if="${currentPage < totalPages - 1}"
       th:href="@{/seller/posts/{id}/credentials(id=${post.postId}, page=${currentPage + 1}, size=${pageSize}, keyword=${keyword}, status=${selectedStatus}, sort=${currentSort}, direction=${currentDirection})}"
       class="px-3 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
        <i class="ph-bold ph-caret-right"></i>
    </a>
    <span th:if="${currentPage >= totalPages - 1}" class="px-3 py-2 text-gray-300 cursor-not-allowed">
        <i class="ph-bold ph-caret-right"></i>
    </span>
</div>
```

Add JavaScript for page size change:

```html
<script th:inline="javascript">
    const postId = /*[[${post.postId}]]*/ 0;

    function changePageSize(size) {
        const url = new URL(window.location);
        url.searchParams.set('size', size);
        url.searchParams.set('page', '0'); // Reset to first page
        window.location.href = url.toString();
    }
    
    // ... existing JavaScript ...
</script>
```

---

## Files to Modify

### Java Files
1. `src/main/java/com/group3/accounttrade/controller/SellerController.java`
   - Update `listSellerPosts()` method
   - Update `manageCredentials()` method

2. `src/main/java/com/group3/accounttrade/service/PostService.java`
   - Add `getCredentialsByPostIdWithFilters()` method
   - Update `getSellerPostsWithFilters()` to accept StockStatus enum

3. `src/main/java/com/group3/accounttrade/repository/PostCredentialRepository.java`
   - Add pagination query methods

4. `src/main/java/com/group3/accounttrade/repository/PostRepository.java`
   - Update query to include stockStatus filter

### Template Files
1. `src/main/resources/templates/seller_posts.html`
   - Add sortable column headers
   - Add page size selector
   - Add results summary
   - Update pagination links to include all parameters

2. `src/main/resources/templates/seller_post_credentials.html`
   - Add search/filter form
   - Add results summary and page size selector
   - Add pagination controls
   - Update existing credential list to use paginated data

---

## UI Component Specifications

### Sortable Column Header
- Click to sort ascending
- Click again to sort descending
- Show arrow indicator (↑ or ↓) for current sort column
- Preserve other filters when sorting

### Page Size Selector
- Dropdown with options: 10, 25, 50
- Changing resets to page 0
- Preserve all filter parameters

### Results Summary
- Format: "Hiển thị X đến Y trong tổng số Z kết quả"
- X = (page * size) + 1 (or 0 if empty)
- Y = min((page + 1) * size, totalElements)
- Z = totalElements

### Pagination Controls
- Previous/Next arrows
- Page number buttons
- Current page highlighted
- Preserve all filter and sort parameters in links

---

## Testing Checklist

- [ ] Post Management: Search by keyword works
- [ ] Post Management: Filter by category works
- [ ] Post Management: Filter by stock status works (was broken before)
- [ ] Post Management: Sorting by each column works
- [ ] Post Management: Page size change works
- [ ] Post Management: Pagination preserves filters
- [ ] Post Management: Results summary displays correctly

- [ ] Credential Management: Search by username works
- [ ] Credential Management: Filter by status works
- [ ] Credential Management: Pagination works
- [ ] Credential Management: Page size change works
- [ ] Credential Management: Pagination preserves filters
- [ ] Credential Management: Results summary displays correctly
- [ ] Credential Management: Add/Edit/Delete still works after pagination
