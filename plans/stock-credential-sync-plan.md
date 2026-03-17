# Stock/Credential Synchronization Enhancement Plan

## Overview

Enhance the seller hub to prominently display the relationship between credentials and stock, ensuring sellers always understand that:

- **Stock = Number of Available Credentials**
- Adding credentials increases stock
- Removing credentials decreases stock
- Sold credentials don't count toward available stock

## Current State

The system already:

- Derives stock status from credentials count
- Shows credential statistics on the edit page
- Has a dedicated credential management page

## Required Enhancements

### 1. Create Post Page (`seller_create_post.html`)

**Changes:**

- Remove any manual stock/quantity input
- Show real-time credential count as "Stock"
- Add validation: Must have at least 1 credential to create post
- Display helpful messaging about stock/credential relationship

**UI Elements:**

```
┌─────────────────────────────────────────────────┐
│ Tài khoản bán hàng (Kho hàng)                   │
│ ┌─────────────────────────────────────────────┐ │
│ │ Stock hiện tại: 3 tài khoản                 │ │
│ │ [Thêm tài khoản]                            │ │
│ │                                             │ │
│ │ ┌─────────────────────────────────────────┐ │ │
│ │ │ Tài khoản #1                             │ │ │
│ │ │ Username: [________] Password: [________]│ │ │
│ │ │ Ghi chú: [____________________________] │ │ │
│ │ │ [Xóa]                                    │ │ │
│ │ └─────────────────────────────────────────┘ │ │
│ │ ┌─────────────────────────────────────────┐ │ │
│ │ │ Tài khoản #2                             │ │ │
│ │ │ ...                                      │ │ │
│ │ └─────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────┘ │
│                                                 │
│ ⚠️ Cảnh báo: Cần ít nhất 1 tài khoản để đăng   │
└─────────────────────────────────────────────────┘
```

### 2. Edit Post Page (`seller_edit_post.html`)

**Changes:**

- Show stock summary prominently
- Display credential count breakdown:
  - Total credentials
  - Available (in stock)
  - Sold (not counted in stock)
- Add quick link to manage credentials
- Remove any manual stock input fields

**UI Elements:**

```
┌─────────────────────────────────────────────────┐
│ Quản lý tài khoản (Kho hàng)                    │
│                                                 │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐            │
│ │ Tổng: 5 │ │ Còn: 3  │ │ Đã bán: 2│            │
│ └─────────┘ └─────────┘ └─────────┘            │
│                                                 │
│ Trạng thái kho: ● Còn hàng (3 tài khoản)       │
│                                                 │
│ [Quản lý tài khoản]                             │
│                                                 │
│ ℹ️ Stock được tính từ số tài khoản có sẵn.      │
│ Mỗi người mua nhận 1 tài khoản duy nhất.        │
└─────────────────────────────────────────────────┘
```

### 3. Credential Management Page (`seller_post_credentials.html`)

**Changes:**

- Show real-time stock status updates
- Display available count prominently
- Add visual feedback when adding/removing credentials
- Show warning when removing last available credential

**UI Elements:**

```
┌─────────────────────────────────────────────────┐
│ Quản lý tài khoản                               │
│                                                 │
│ Stock hiện tại: 3 tài khoản có sẵn              │
│                                                 │
│ ┌─────────────────────────────────────────────┐ │
│ │ Thêm tài khoản mới                          │ │
│ │ ...                                         │ │
│ └─────────────────────────────────────────────┘ │
│                                                 │
│ ┌─────────────────────────────────────────────┐ │
│ │ Danh sách tài khoản                         │ │
│ │                                             │ │
│ │ #1 user1@email.com ● Có sẵn    [Edit][Del] │ │
│ │ #2 user2@email.com ● Có sẵn    [Edit][Del] │ │
│ │ #3 user3@email.com ● Có sẵn    [Edit][Del] │ │
│ │ #4 user4@email.com ● Đã bán               │ │
│ │ #5 user5@email.com ● Đã bán               │ │
│ └─────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

### 4. Validation Rules

**Create Post:**

- Must have at least 1 credential with valid username and password
- If no valid credentials, show error: "Vui lòng thêm ít nhất 1 tài khoản để đăng bán"

**Edit Post:**

- Cannot manually change stock (it's derived)
- Show warning if trying to delete last available credential

**Purchase:**

- Only allow purchase if available credentials > 0
- Assign credential atomically with transaction

### 5. Service Layer Updates

**PostService.java:**

```java
// Ensure stock status is always updated after credential changes
@Transactional
public void updateStockStatus(Integer postId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new IllegalArgumentException("Post not found"));

    long availableCount = postCredentialRepository
        .countByPost_PostIdAndCredentialStatus(postId, CredentialStatus.AVAILABLE);

    StockStatus newStatus = availableCount > 0
        ? StockStatus.IN_STOCK
        : StockStatus.OUT_OF_STOCK;

    if (post.getStockStatus() != newStatus) {
        post.setStockStatus(newStatus);
        postRepository.save(post);
    }
}

// Get detailed stock info
public StockInfo getStockInfo(Integer postId) {
    long total = postCredentialRepository.countByPost_PostId(postId);
    long available = postCredentialRepository
        .countByPost_PostIdAndCredentialStatus(postId, CredentialStatus.AVAILABLE);
    long sold = postCredentialRepository
        .countByPost_PostIdAndCredentialStatus(postId, CredentialStatus.SOLD);

    return new StockInfo(total, available, sold);
}
```

### 6. Controller Updates

**SellerController.java:**

- Add `@GetMapping("/posts/{postId}/stock-info")` endpoint for AJAX stock updates
- Return JSON with stock info for real-time UI updates

### 7. JavaScript Enhancements

**Real-time Stock Counter:**

```javascript
function updateStockDisplay() {
  const credentials = document.querySelectorAll(".credential-row");
  const availableCount = Array.from(credentials).filter(
    (c) => c.dataset.status === "AVAILABLE",
  ).length;

  document.getElementById("stockCount").textContent = availableCount;
  document.getElementById("stockStatus").className =
    availableCount > 0 ? "text-success" : "text-error";
  document.getElementById("stockStatus").textContent =
    availableCount > 0 ? "Còn hàng" : "Hết hàng";
}
```

## Implementation Order

1. **Update PostService** - Add `getStockInfo()` method
2. **Update SellerController** - Add stock info endpoint
3. **Update seller_create_post.html** - Real-time credential counter
4. **Update seller_edit_post.html** - Stock summary display
5. **Update seller_post_credentials.html** - Real-time stock updates
6. **Add JavaScript** - Stock counter and validation
7. **Testing** - Verify synchronization

## Files to Modify

1. `src/main/java/com/group3/accounttrade/service/PostService.java`
2. `src/main/java/com/group3/accounttrade/controller/SellerController.java`
3. `src/main/resources/templates/seller_create_post.html`
4. `src/main/resources/templates/seller_edit_post.html`
5. `src/main/resources/templates/seller_post_credentials.html`
