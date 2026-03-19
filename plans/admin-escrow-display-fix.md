# Admin Dashboard Escrow Display Fix Plan

## Problem Statement

The admin dashboard's "Đang tạm giữ (Escrow)" section shows "0 giao dịch" (0 transactions) even when there are pending orders that should be displayed in escrow.

## Root Cause Analysis

### Status Name Mismatch

The issue is a **status name mismatch** between the query and the database:

**In [`AdminDashboardService.java:60-64`](src/main/java/com/group3/accounttrade/service/AdminDashboardService.java:60):**

```java
BigDecimal escrowHoldings = escrowRepository.sumTotalAmountByStatus("HELD");
long escrowCount = escrowRepository.countByStatusName("HELD");
```

**But the actual status in [`EscrowStatus.java:32`](src/main/java/com/group3/accounttrade/entity/EscrowStatus.java:32):**

```java
public static final String HOLDING = "HOLDING";
```

**And seeded in [`DataInitializer.java:184-187`](src/main/java/com/group3/accounttrade/config/DataInitializer.java:184):**

```java
escrowStatusRepository.save(EscrowStatus.builder()
    .statusName(EscrowStatus.HOLDING)  // "HOLDING"
    .description("Funds are being held in escrow")
    .build());
```

The service queries for status `"HELD"` but the database contains status `"HOLDING"`.

## Solution

### Change Required

Update [`AdminDashboardService.java`](src/main/java/com/group3/accounttrade/service/AdminDashboardService.java) to use the correct status name:

**Line 60:** Change `"HELD"` → `"HOLDING"`
**Line 64:** Change `"HELD"` → `"HOLDING"`

### Code Fix

```java
// Before:
BigDecimal escrowHoldings = escrowRepository.sumTotalAmountByStatus("HELD");
long escrowCount = escrowRepository.countByStatusName("HELD");

// After:
BigDecimal escrowHoldings = escrowRepository.sumTotalAmountByStatus(EscrowStatus.HOLDING);
long escrowCount = escrowRepository.countByStatusName(EscrowStatus.HOLDING);
```

## Implementation Steps

1. Open [`AdminDashboardService.java`](src/main/java/com/group3/accounttrade/service/AdminDashboardService.java)
2. Update line60 to use `EscrowStatus.HOLDING` constant instead of `"HELD"`
3. Update line64 to use `EscrowStatus.HOLDING` constant instead of `"HELD"`
4. Verify the import for `EscrowStatus` is present

## Verification

After the fix:

1. Restart the application
2. Navigate to admin dashboard
3. The "Đang tạm giữ (Escrow)" section should display the correct count and amount for orders in HOLDING status

## Related Files

- [`AdminDashboardService.java`](src/main/java/com/group3/accounttrade/service/AdminDashboardService.java) - Service with the bug
- [`EscrowStatus.java`](src/main/java/com/group3/accounttrade/entity/EscrowStatus.java) - Entity with correct constant
- [`EscrowRepository.java`](src/main/java/com/group3/accounttrade/repository/EscrowRepository.java) - Repository queries
- [`admin_dashboard.html`](src/main/resources/templates/admin_dashboard.html) - Frontend template
