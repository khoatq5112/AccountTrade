# Admin Transactions Detail 404 Fix Plan

## Problem Statement

When viewing transaction details in the Admin panel:
- **TXN-ESCROW-23** works correctly
- **TXN-PAYMENT-18** returns HTTP 404 error

## Root Cause Analysis

### 1. Current Flow

```
User clicks View Details 
    → Frontend calls /api/admin/transactions/{id}
    → AdminDashboardController.getTransactionDetail()
    → AdminTransactionService.getTransactionById()
    → Returns Optional.empty() → 404
```

### 2. Why Some Transactions Fail

In [`AdminTransactionService.getTransactionById()`](src/main/java/com/group3/accounttrade/service/AdminTransactionService.java:90-126):

```java
@Transactional(readOnly = true)
public Optional<TransactionDTO> getTransactionById(String transactionId) {
    try {
        if (transactionId.startsWith("TXN-PAYMENT-")) {
            Long id = Long.parseLong(transactionId.substring(13));
            Optional<Payment> p = paymentRepository.findByIdWithDetails(id);
            if (p.isPresent()) return Optional.of(mapPayment(p.get()));
        }
        // ... other types
    } catch (NumberFormatException e) {
        log.warn("Invalid transaction ID format: {}", transactionId);
    } catch (org.hibernate.LazyInitializationException e) {
        log.error("LazyInitializationException while loading transaction {}: {}", transactionId, e.getMessage());
        return Optional.empty();  // ← This causes 404!
    }
    return Optional.empty();
}
```

### 3. The JOIN FETCH Query Issue

The [`PaymentRepository.findByIdWithDetails()`](src/main/java/com/group3/accounttrade/repository/PaymentRepository.java:44-52) query:

```java
@Query("SELECT p FROM Payment p " +
       "LEFT JOIN FETCH p.order o " +
       "LEFT JOIN FETCH o.buyer " +
       "LEFT JOIN FETCH o.orderItems oi " +
       "LEFT JOIN FETCH oi.post post " +
       "LEFT JOIN FETCH post.seller " +  // ← Problem: if post is null, this fails
       "LEFT JOIN FETCH p.paymentStatus " +
       "WHERE p.paymentId = :id")
Optional<Payment> findByIdWithDetails(Long id);
```

**The Issue:** When an order item has no post (post_id is null), the query tries to JOIN FETCH `post.seller` but `post` is null. This can cause:
1. The query to return no results even though the payment exists
2. LazyInitializationException when trying to access seller in the mapping code

### 4. Why List View Works but Detail View Fails

- **List View** uses `paymentRepository.findAll()` with Open Session in View pattern
- **Detail View** uses custom JOIN FETCH query that may fail for payments with incomplete data

## Solution

### Option 1: Fix the JOIN FETCH Query (Recommended)

Split the query to handle optional relationships:

```java
@Query("SELECT p FROM Payment p " +
       "LEFT JOIN FETCH p.order o " +
       "LEFT JOIN FETCH o.buyer " +
       "LEFT JOIN FETCH o.orderItems oi " +
       "LEFT JOIN FETCH oi.post post " +
       "LEFT JOIN FETCH p.paymentStatus " +
       "WHERE p.paymentId = :id")
Optional<Payment> findByIdWithDetails(Long id);
```

Then handle seller loading separately in the service:

```java
// In AdminTransactionService
private TransactionDTO mapPayment(Payment p) {
    Order order = p.getOrder();
    User buyer = order != null ? order.getBuyer() : null;
    User seller = getSellerFromOrder(order);  // Handle null post gracefully
    // ...
}

private User getSellerFromOrder(Order order) {
    if (order == null || order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
        return null;
    }
    OrderItem item = order.getOrderItems().get(0);
    if (item != null && item.getPost() != null) {
        return item.getPost().getSeller();  // May trigger lazy load, but within transaction
    }
    return null;
}
```

### Option 2: Remove LazyInitializationException Catch

Don't silently return empty - let the exception propagate for proper debugging:

```java
@Transactional(readOnly = true)
public Optional<TransactionDTO> getTransactionById(String transactionId) {
    // ... validation ...
    
    if (transactionId.startsWith("TXN-PAYMENT-")) {
        Long id = Long.parseLong(transactionId.substring(13));
        return paymentRepository.findByIdWithDetails(id)
            .map(this::mapPayment);  // Let exceptions propagate
    }
    // ...
}
```

### Option 3: Use EntityGraph (Alternative)

```java
@EntityGraph(attributePaths = {
    "order", "order.buyer", "order.orderItems", 
    "order.orderItems.post", "paymentStatus"
})
Optional<Payment> findById(Long id);
```

## Implementation Steps

1. **Modify PaymentRepository.findByIdWithDetails()** - Remove `post.seller` from JOIN FETCH
2. **Update AdminTransactionService.mapPayment()** - Handle null post gracefully
3. **Remove LazyInitializationException catch** - Or convert to proper error logging
4. **Test all transaction types** - ESCROW, PAYMENT, REFUND, ORDER

## Files to Modify

1. [`src/main/java/com/group3/accounttrade/repository/PaymentRepository.java`](src/main/java/com/group3/accounttrade/repository/PaymentRepository.java)
   - Remove `LEFT JOIN FETCH post.seller` from query

2. [`src/main/java/com/group3/accounttrade/service/AdminTransactionService.java`](src/main/java/com/group3/accounttrade/service/AdminTransactionService.java)
   - Update `mapPayment()` to handle null post
   - Remove or improve LazyInitializationException handling

3. Similar fixes may be needed for:
   - [`EscrowTransactionRepository.findByIdWithDetails()`](src/main/java/com/group3/accounttrade/repository/EscrowTransactionRepository.java)
   - [`RefundRequestRepository.findByIdWithDetails()`](src/main/java/com/group3/accounttrade/repository/RefundRequestRepository.java)
