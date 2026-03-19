# Sidebar Inconsistency Fix Plan

## Problem Description

The sidebar in `/seller/dashboard` displays differently from `/seller/posts`:

### Dashboard Sidebar (Complete):

```
Đã xác minh cấp 2
Tổng quan
Quản lý bài đăng
Đăng bài mới
Đơn hàng đang bán
Lịch sử bán hàng
Doanh thu & Rút tiền
Cài đặt gian hàng
Trung tâm khiếu nại
```

### Posts Sidebar (Incomplete):

```
SE
sellerAccount
Đã xác minh
Tổng quan
Quản lý bài đăng
Đăng bài mới
```

## Root Cause Analysis

Comparing [`seller_dashboard.html`](src/main/resources/templates/seller_dashboard.html:127) with [`seller_posts.html`](src/main/resources/templates/seller_posts.html:123):

### Issue 1: Missing Navigation Items

**seller_dashboard.html** (lines 127-160) has 8 navigation items:

- Tổng quan
- Quản lý bài đăng
- Đăng bài mới
- Đơn hàng đang bán
- Lịch sử bán hàng
- Doanh thu & Rút tiền
- Cài đặt gian hàng
- Trung tâm khiếu nại

**seller_posts.html** (lines 123-136) only has 3 navigation items:

- Tổng quan
- Quản lý bài đăng
- Đăng bài mới

### Issue 2: Different Avatar Display

- Dashboard: `${#strings.toUpperCase(currentUser.username.substring(0, 3))}` - Shows 3 characters
- Posts: `${#authentication.name.substring(2).toUpperCase()}` - Shows 2 characters

### Issue 3: Different Verification Text

- Dashboard: "Đã xác minh cấp 2"
- Posts: "Đã xác minh"

### Issue 4: Different User Data Source

- Dashboard uses: `currentUser.username` (from model)
- Posts uses: `#authentication.name` (from Spring Security)

### Issue 5: Missing Wallet Balance in Navigation Bar

- Dashboard has wallet balance display in the nav bar (line 71-74)
- Posts page is missing this feature

## Controller Analysis

Looking at [`SellerController.java`](src/main/java/com/group3/accounttrade/controller/SellerController.java):

### Dashboard Endpoint (lines 42-63):

```java
@GetMapping("/dashboard")
public String viewSellerDashboard(Authentication authentication, Model model) {
    // ...
    User user = userRepository.findByUsername(username).orElse(null);
    model.addAttribute("currentUser", user);  // ✓ Adds currentUser
    // ...
}
```

### Posts Endpoint (lines 130-194):

```java
@GetMapping("/posts")
public String listSellerPosts(..., Model model) {
    // ...
    // ✗ Does NOT add currentUser to model
    // ...
}
```

**Root Cause**: The `/seller/posts` endpoint does not add `currentUser` to the model, so the template cannot access user information the same way as the dashboard.

## Solution

### Option A: Update Controller + Template (Recommended)

1. Add `currentUser` to model in `listSellerPosts()` method
2. Update `seller_posts.html` sidebar to match `seller_dashboard.html`

### Option B: Template Only (Quick Fix)

1. Update `seller_posts.html` to use `#authentication` for user data
2. Add missing navigation items

**Recommended**: Option A for consistency across all seller pages.

## Files to Modify

| File                                                 | Changes                                                  |
| ---------------------------------------------------- | -------------------------------------------------------- |
| `src/main/resources/templates/seller_posts.html`     | Update sidebar section to match seller_dashboard.html    |
| `src/main/java/.../controller/SellerController.java` | Add `currentUser` to model in `listSellerPosts()` method |

## Implementation Steps

1. [x] Analyze sidebar differences between seller_dashboard.html and seller_posts.html
2. [x] Document the root cause and required changes
3. [ ] Update SellerController.java to add currentUser model attribute
4. [ ] Update seller_posts.html sidebar section (lines 105-138)
5. [ ] Test both pages to verify consistency
