# URL ID Encoding Implementation Plan

## Overview

This document outlines a reversible encoding scheme to obfuscate database IDs in URLs, preventing exposure of sensitive internal identifiers while maintaining backend functionality.

## Problem Statement

The application currently exposes raw database IDs in URLs, which can lead to:

- **ID Enumeration Attacks**: Attackers can easily enumerate through sequential IDs to discover all records
- **Information Disclosure**: Reve business metrics, user counts, and competitive intelligence
- **Social Engineering**: URLs with visible IDs can be shared to reveal user activity

## Current URL Patterns Exposing IDs

### Controllers:

1. **BuyerController**:
   - `/buyer/purchases?orderId=`
   - `/buyer/purchases?revealCredentialsOrderId=`
   - `/buyer/orders/{orderId}/confirm`
   - `/buyer/orders/{orderId}/disputes`
   - `/buyer/disputes/{disputeId}`
   - `/buyer/disputes/{disputeId}/messages`
   - `/buyer/disputes/{disputeId}/cancel`
2. **SellerController**:
   - `/seller/posts/{postId}/edit`
   - `/seller/posts/{postId}` (POST update)
   - `/seller/posts/{postId}/delete`
   - `/seller/posts/{postId}/credentials`
   - `/seller/credentials/{credentialId}` (POST update)
   - `/seller/credentials/{credentialId}/delete`
   - `/seller/disputes/{disputeId}`
   - `/seller/disputes/{disputeId}/response`
   - `/seller/disputes/{disputeId}/messages`

3. **MarketplaceController**:
   - `/marketplace/{postId}`
   - `/marketplace?categoryId=...` (less sensitive)
     `
4. **AdminDashboardController**:
   - `/api/admin/posts/{postId}/approve`
   - `/api/admin/posts/{postId}/reject`
   - `/api/admin/disputes/{disputeId}`
   - `/api/admin/disputes/{disputeId}/assign`
   - `/api/admin/disputes/{disputeId}/review`
   - `/api/admin/disputes/{disputeId}/resolve/buyer`
   - `/api/admin/disputes/{disputeId}/resolve/seller`
   - `/api/admin/disputes/{disputeId}/notes`
   - `/api/admin/categories/{categoryId}/commission`
   - `/api/admin/transactions/{transactionId}`

5. **CartController**:
   - `/api/cart/add?postId=`
   - `/api/cart/remove/{postId}`

6. **PaymentController**:
   - `/payment/status/{orderId}`

### Templates (53+ occurrences in 17 templates)

- marketplace.html, marketplace_detail.html, cart.html, checkout.html
  buyer_purchases.html, buyer_disputes.html, buyer_dispute_detail.html, seller_posts.html, seller_post_credentials.html, seller_edit_post.html, seller_orders.html, seller_disputes.html, seller_dispute_detail.html, admin_disputes.html, admin_dispute_detail.html, admin_transactions.html, payment_result.html, payment_status.html

## Proposed Solution: Hashids-Based ID Encoding

### Why Hashids?

- **Reversible**: Can encode and decode IDs back to their original values
- **Non-sequential**: Encoded IDs don't follow a predictable pattern
- **Customizable**: Uses a salt and minimum hash length for added security
- **URL-safe**: Produ URL-safe strings without special characters
- **Spring-friendly**: Easy integration with Thymeleaf and Spring controllers

- **Well-tested**: Popular library with proven track record

### Implementation Components

1. **IdEncoder Service** - Central encoding/decoding utility
2. **IdEncoder Configuration** - Application properties for salt
   minimum hash length
3. **Controller Updates** - Accept encoded IDs, decode them, process
4. **Template Updates** - Use encoded IDs in URLs
5. **JavaScript Updates** - Use encoded IDs in AJAX calls
6. **Tests** - Verify encoding/decoding works correctly

7. **Migration** - Database migration for any schema changes (if needed)

## Technical Design

### Encoding Algorithm

```
ID → Hashids.encode(id) → "encoded_string"
```

"encoded_string" → Hashids.decode("encoded_string") → ID

````

Example:
- Input: `123` → Output: `"jR2X7kL"` (example)
- Decode: `"jR2X7kL"` → Output: `123`

### Configuration
```properties
# Hashids configuration
app.id-encoder.salt=your-secret-salt-here
app.id-encoder.min-hash-length=8

## ID Types to Encode
The | ID Type | Prefix | Example |
    |------------|--------|---------|
    | postId | post | post_123 → post_jR2X7kL |
    | orderId | order | order_123 → order_jR2X7kL |
    | disputeId | dispute | dispute_123 → dispute_jR2X7kL |
    | credentialId | cred | cred_123 → cred_jR2X7kL |
    | categoryId | cat | cat_123 → cat_jR2X7kL |
    | transactionId | txn | txn_123 → txn_jR2X7kL |

## File Structure

````

src/main/java/com/group3/accounttrade/
└── util/
└── IdEncoder.java

````

## Dependencies
```xml
<dependency>
    <groupId>org.hashids</groupId>
    <artifactId>hashids</artifactId>
    <version>1.0.3</version>
</dependency>
````

## Testing Strategy

1. **Unit Tests**: Test encoding/decoding for each ID type
2. **Integration Tests**: Test controller endpoints with encoded IDs
3. **Manual Testing**: Verify URLs in browser show encoded IDs

4. **Backward Compatibility**: Ensure existing bookmarks/links still work

## Migration Strategy

1. **Phase 1**: Add Hashids dependency and create IdEncoder utility
2. **Phase 2**: Add configuration properties
3. **Phase 3**: Update controllers to use encoded IDs
4. **Phase 4**: Update templates to use encoded IDs
5. **Phase 5**: Update JavaScript to use encoded IDs
6. **Phase 6**: Test thoroughly
7. **Phase 7**: Deploy and monitor

## Rollback Plan

- Keep original numeric IDs available in controllers as fallback
- Can be disabled via configuration if needed
- Monitor for any issues with encoded URLs
- Can quickly rollback by removing Hashids configuration

## Future Enhancements

- Consider adding a caching layer for frequently decoded IDs
- Explore using JWT tokens for stateless encoding
- Consider rate limiting for encoding endpoints
