# Admin Transactions Refactoring Plan

## Objective

Refactor the AdminTransactionService to reuse existing transaction history logic and data retrieval patterns from BuyerOrderService and SellerDashboardService, ensuring consistent data mapping and resolving LazyInitialization errors.

## Current Issues

1. **LazyInitializationException** - Manual JOIN FETCH queries fail when optional relationships are null
2. **Inconsistent patterns** - AdminTransactionService uses different query patterns than other services
3. **Silent failures** - Exceptions are caught and return empty, causing 404 errors

## Existing Patterns to Follow

### 1. OrderItemRepository Pattern (Recommended)

```java
@EntityGraph(attributePaths = {"order", "order.buyer", "order.orderStatus", "post", "seller"})
@Query("SELECT oi FROM OrderItem oi ...")
Page<OrderItem> findSellerOrderItems(User seller, String keyword, String status, Pageable pageable);
```

**Benefits:**
- `@EntityGraph` handles null relationships gracefully
- Eager loads only necessary relationships
- Consistent with SellerDashboardService

### 2. SellerDashboardService Mapping Pattern

```java
private SellerOrderRow toSellerOrderRow(OrderItem orderItem) {
    Order order = orderItem.getOrder();
    return new SellerOrderRow(
        order.getOrderId(),
        order.getOrderNumber(),
        orderItem.getPost() != null ? orderItem.getPost().getPostId() : null,  // Null-safe
        orderItem.getPostTitleSnapshot(),
        order.getBuyer() != null ? order.getBuyer().getUsername() : "Unknown buyer",  // Null-safe
        // ...
    );
}
```

**Benefits:**
- Null-safe field access
- Uses snapshots (postTitleSnapshot) instead of lazy-loaded relationships
- Simple record-based DTOs

## Refactoring Strategy

### Phase 1: Update Repository Queries

Replace manual JOIN FETCH with @EntityGraph in:

1. **PaymentRepository**
```java
@EntityGraph(attributePaths = {"order", "order.buyer", "order.orderItems", "order.orderItems.post", "paymentStatus"})
Optional<Payment> findById(Long id);
```

2. **EscrowTransactionRepository**
```java
@EntityGraph(attributePaths = {"escrow", "escrow.order", "escrow.order.buyer", "escrow.escrowStatus"})
Optional<EscrowTransaction> findById(Long id);
```

3. **RefundRequestRepository**
```java
@EntityGraph(attributePaths = {"order", "order.buyer", "dispute"})
Optional<RefundRequest> findById(Long id);
```

### Phase 2: Refactor AdminTransactionService

1. **Remove custom JOIN FETCH queries** - Use standard `findById()` with EntityGraph

2. **Update mapping methods** to be null-safe:
```java
private TransactionDTO mapPayment(Payment p) {
    Order order = p.getOrder();
    User buyer = order != null ? order.getBuyer() : null;
    User seller = getSellerFromOrder(order);  // Null-safe helper
    
    return new TransactionDTO(
        "TXN-PAYMENT-" + p.getPaymentId(),
        "PAYMENT_" + getStatusName(p.getPaymentStatus()),
        "PAYMENT",
        getStatusName(p.getPaymentStatus()),
        p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO,
        toUserInfo(buyer),
        toUserInfo(seller),
        toOrderInfo(order),
        p.getCreatedAt(),
        p.getUpdatedAt(),
        "Payment via VNPAY"
    );
}
```

3. **Remove LazyInitializationException catch block** - Let errors propagate for debugging

4. **Add null-safe helpers**:
```java
private String getStatusName(PaymentStatus status) {
    return status != null ? status.getStatusName() : "PENDING";
}

private User getSellerFromOrder(Order order) {
    if (order == null || order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
        return null;
    }
    OrderItem item = order.getOrderItems().get(0);
    // Use seller field directly instead of post.seller
    return item.getSeller();
}
```

### Phase 3: Consolidate Transaction Types

Consider unifying transaction views around OrderItem like SellerDashboardService:

```java
public record AdminTransactionRow(
    String transactionId,
    String transactionType,
    String category,
    String status,
    BigDecimal amount,
    UserInfo buyer,
    UserInfo seller,
    OrderInfo order,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String description
) {}
```

## Implementation Steps

### Step 1: Update PaymentRepository
- Remove `findByIdWithDetails()` method
- Add `@EntityGraph` to standard `findById()`

### Step 2: Update EscrowTransactionRepository
- Remove custom `findByIdWithDetails()` method
- Add `@EntityGraph` annotation

### Step 3: Update RefundRequestRepository
- Remove custom `findByIdWithDetails()` method  
- Add `@EntityGraph` annotation

### Step 4: Update AdminTransactionService
- Use standard `findById()` methods
- Update mapping methods to be null-safe
- Remove LazyInitializationException catch

### Step 5: Test All Transaction Types
- ESCROW transactions
- PAYMENT transactions
- REFUND transactions
- ORDER transactions

## Files to Modify

1. `src/main/java/com/group3/accounttrade/repository/PaymentRepository.java`
2. `src/main/java/com/group3/accounttrade/repository/EscrowTransactionRepository.java`
3. `src/main/java/com/group3/accounttrade/repository/RefundRequestRepository.java`
4. `src/main/java/com/group3/accounttrade/repository/OrderRepository.java`
5. `src/main/java/com/group3/accounttrade/service/AdminTransactionService.java`

## Expected Outcomes

1. **No more 404 errors** - EntityGraph handles null relationships gracefully
2. **Consistent patterns** - Same approach as SellerDashboardService
3. **Better error handling** - Real errors surface instead of being hidden
4. **Maintainable code** - Less custom query duplication
3. `src/main/java/com/group3/accounttrade/repository/RefundRequestRepository.java`
4. `src/main/java/com/group3/accounttrade/repository/OrderRepository.java`
5. `src/main/java/com/group3/accounttrade/service/AdminTransactionService.java`

## Expected Outcomes

1. **No more 404 errors** - EntityGraph handles null relationships gracefully
2. **Consistent patterns** - Same approach as SellerDashboardService
3. **Better error handling** - Real errors surface instead of being hidden
4. **Maintainable code** - Less custom query duplication
