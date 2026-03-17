# Account Marketplace System - Refactoring Plan

## Overview
This plan outlines the refactoring and improvements needed for the Account Marketplace system.

---

## 1. Database Refactor - Simplify Categories

### Current State
- Categories have parent-child relationships (hierarchical)
- Sellers must select from subcategories only

### Target State
- Single-level categories (no parent-child)
- Simple category selection

### New Categories
- Giải trí
- Nghe Nhạc
- Xem Phim
- Khác

### Changes Required

| File | Change |
|------|--------|
| `Category.java` | Remove `parent` and `subcategories` fields |
| `CategoryRepository.java` | Remove subcategory methods, add simple findAll |
| `schema.sql` | Remove parent_id column, update seed data |
| `PostService.java` | Remove subcategory validation |
| `SellerController.java` | Use simple category list |
| `seller_create_post.html` | Simple dropdown without optgroups |

---

## 2. Marketplace Page Improvements

### 2.1 Product Card - Add Seller Name
**File**: `marketplace.html`

Add seller name display:
```html
<div class="flex items-center gap-2 mb-3 text-xs text-gray-500">
    <div class="w-5 h-5 bg-primary/10 rounded-full flex items-center justify-center">
        <i class="ph-fill ph-user text-primary text-xs"></i>
    </div>
    <span th:text="${post.seller.username}">Seller Name</span>
</div>
```

---

## 3. Product Details Page Fix

### 3.1 HTML Rendering Issue
**File**: `marketplace_detail.html`

**Problem**: Description uses `th:text` which escapes HTML
**Solution**: Use `th:utext` to render HTML content

Change line 98:
```html
<!-- FROM -->
<p th:text="${post.description}">Description</p>

<!-- TO -->
<div th:utext="${post.description}" class="prose max-w-none">Description</div>
```

### 3.2 Add Created Date
Add created date display to product details page.

---

## 4. Placeholder Image Handling

### 4.1 Default Thumbnail
**File**: `Post.java`

The `getResolvedThumbnailUrl()` method already handles this by generating an SVG placeholder.

Alternative: Use a static placeholder image:
```java
@Transient
public String getResolvedThumbnailUrl() {
    if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
        return thumbnailUrl;
    }
    return "/images/placeholder-product.png";
}
```

---

## 5. Seller Posts Management Page

### 5.1 New Endpoint
**Path**: `/seller/posts`

### 5.2 Features
- Table view of all seller's posts
- Search by title
- Filter by category and status
- Pagination (10 posts per page)
- Actions: View, Edit, Delete

### 5.3 Files to Create/Modify

| File | Action |
|------|--------|
| `SellerController.java` | Add GET `/seller/posts` endpoint |
| `PostService.java` | Add methods for seller posts with pagination/search |
| `PostRepository.java` | Add query methods |
| `seller_posts.html` | New template for posts table |

### 5.4 Controller Method
```java
@GetMapping("/posts")
public String listSellerPosts(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(required = false) Integer categoryId,
    @RequestParam(required = false) Integer statusId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    Authentication authentication,
    Model model) {
    // Implementation
}
```

---

## 6. Implementation Order

1. **Category Simplification**
   - Update Category entity
   - Update CategoryRepository
   - Update schema.sql
   - Update PostService
   - Update SellerController
   - Update seller_create_post.html

2. **Marketplace Improvements**
   - Add seller name to product cards
   - Update marketplace.html

3. **Product Details Fix**
   - Change th:text to th:utext for description
   - Add created date display

4. **Seller Posts Management**
   - Add PostRepository methods
   - Add PostService methods
   - Add SellerController endpoint
   - Create seller_posts.html template

5. **Testing**
   - Compile and verify all changes

---

## Files Summary

### Files to Modify
1. `Category.java` - Remove parent-child fields
2. `CategoryRepository.java` - Simplify queries
3. `PostService.java` - Remove subcategory validation, add seller posts methods
4. `SellerController.java` - Simplify category handling, add posts list endpoint
5. `seller_create_post.html` - Simple category dropdown
6. `marketplace.html` - Add seller name to cards
7. `marketplace_detail.html` - Fix HTML rendering with th:utext
8. `schema.sql` - Update categories table and seed data

### Files to Create
1. `seller_posts.html` - Seller posts management page

---

## Questions for Clarification

1. **Data Migration**: Should we migrate existing posts to new categories, or create a fresh database?

2. **Edit/Delete Posts**: Should sellers be able to edit and delete their posts?

3. **Image Placeholder**: Use static placeholder image or keep dynamic SVG generation?

4. **Post Status Flow**: What statuses can sellers set? (Available, Hidden only, or all?)
