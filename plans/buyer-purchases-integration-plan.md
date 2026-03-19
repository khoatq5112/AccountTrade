# Buyer Purchases Page Integration Plan

## Overview

The **Lịch sử mua hàng** (Purchase History) page is currently implemented as a standalone page outside the Buyer Dashboard structure. This plan outlines the steps to integrate it within the Buyer Dashboard, ensuring consistent layout, navigation, and styling.

## Current State Analysis

### Existing Files

| File                                                                                            | Purpose                       | Current State                             |
| ----------------------------------------------------------------------------------------------- | ----------------------------- | ----------------------------------------- |
| [`buyer_dashboard.html`](src/main/resources/templates/buyer_dashboard.html)                     | Main Buyer Dashboard template | Complete layout with nav, sidebar, footer |
| [`buyer_purchases.html`](src/main/resources/templates/buyer_purchases.html)                     | Purchase history page         | Standalone page without dashboard layout  |
| [`BuyerController.java`](src/main/java/com/group3/accounttrade/controller/BuyerController.java) | Controller for buyer routes   | `/buyer/purchases` endpoint exists        |

### Current buyer_purchases.html Structure

```
└── <main> (standalone)
    ├── Header section (title + back link)
    ├── Alert messages
    ├── Empty state
    └── Order list with credentials reveal
```

### Target buyer_dashboard.html Structure

```
└── Full page
    ├── <nav> (navigation bar)
    ├── Dashboard content wrapper
    │   ├── <aside> (sidebar)
    │   └── <main> (content area)
    └── <footer>
```

## Gap Analysis

### Missing Elements in buyer_purchases.html

1. **Navigation Bar** - Top navigation with logo, menu links, wallet balance, user menu
2. **Sidebar** - User profile card + navigation menu
3. **Footer** - Site footer with branding
4. **Shared Styling** - Tailwind config, CSS classes, Phosphor icons
5. **Active State** - Sidebar should highlight Order History as active

### Data Requirements

The controller already provides:

- `workflowOrders` - List of orders
- `highlightOrderId` - Order to highlight
- `revealedCredentialsOrderId` - Order with revealed credentials
- `revealedCredentials` - Credential data

Additional data needed for layout:

- `currentUser` - User object for nav/sidebar
- `walletBalance` - For wallet display in nav

## Implementation Plan

### Step 1: Update BuyerController Data Model

Add required attributes for the dashboard layout:

```java
@GetMapping("/purchases")
public String viewPurchases(...) {
    // ... existing code ...

    // Add dashboard layout attributes
    User user = getCurrentUser();
    model.addAttribute("currentUser", user);

    Wallet wallet = walletRepository.findByUser_UserId(user.getUserId()).orElse(null);
    BigDecimal walletBalance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;
    model.addAttribute("walletBalance", walletBalance);

    return "buyer_purchases";
}
```

### Step 2: Refactor buyer_purchases.html Template

Restructure the template to match buyer_dashboard.html layout:

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="...">
  <head>
    <!-- Same head content as buyer_dashboard.html -->
  </head>
  <body>
    <!-- Same nav as buyer_dashboard.html -->
    <nav>...</nav>

    <!-- Dashboard Content -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="flex flex-col md:flex-row gap-8">
        <!-- Same sidebar as buyer_dashboard.html -->
        <aside>...</aside>

        <!-- Main Content - Purchases specific -->
        <main class="flex-1 space-y-6">
          <!-- Page header -->
          <!-- Order list -->
          <!-- Credentials reveal section -->
        </main>
      </div>
    </div>

    <!-- Same footer as buyer_dashboard.html -->
    <footer>...</footer>

    <!-- User menu toggle script -->
  </body>
</html>
```

### Step 3: Update Sidebar Navigation Active State

Modify sidebar to highlight Order History when on purchases page:

```html
<!-- Overview - not active -->
<a
  th:href="@{/buyer/dashboard}"
  class="flex items-center gap-3 px-4 py-3 text-gray-600 hover:bg-gray-50..."
>
  <i class="ph ph-squares-four text-lg"></i> Tổng quan
</a>

<!-- Order History - ACTIVE -->
<a
  th:href="@{/buyer/purchases}"
  class="flex items-center gap-3 px-4 py-3 bg-blue-50 text-primary font-bold rounded-lg..."
>
  <i class="ph-fill ph-clock-counter-clockwise text-lg"></i> Lịch sử giao dịch
</a>
```

### Step 4: Preserve Credential Reveal Functionality

Ensure the existing credential reveal feature works correctly:

- Keep the reveal button logic
- Maintain the credentials display section
- Preserve view count and audit logging

## File Changes Summary

### Files to Modify

| File                                                                                                | Changes                                        |
| --------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| [`BuyerController.java`](src/main/java/com/group3/accounttrade/controller/BuyerController.java:155) | Add `currentUser` and `walletBalance` to model |
| [`buyer_purchases.html`](src/main/resources/templates/buyer_purchases.html)                         | Complete restructure with dashboard layout     |

### No New Files Required

The integration uses existing patterns from buyer_dashboard.html.

## Visual Comparison

### Before (Current)

```
┌──────────────────────────────────────────┐
│ Lịch sử mua hàng                         │
│ ─────────────────────────────────────────│
│ Order #1                                 │
│ Order #2                                 │
│ Order #3                                 │
└──────────────────────────────────────────┘
```

### After (Integrated)

```
┌─────────────────────────────────────────────────────────────┐
│ NAV: Logo | Marketplace | Dashboard | Support | Wallet | User│
├──────────────┬──────────────────────────────────────────────┤
│   SIDEBAR    │  Lịch sử mua hàng                            │
│   ────────   │  ─────────────────────────────────────────── │
│   [Avatar]   │                                              │
│   Username   │  ┌────────────────────────────────────────┐ │
│   ────────   │  │ Order #ORD-xxx                         │ │
│   > Tổng quan│  │ Product Name | Seller | Amount         │ │
│   > Đơn hàng │  │ Status: AWAITING_xxx                   │ │
│   [✓] Lịch sử│  │ [Xem credentials]                      │ │
│   > Ví       │  └────────────────────────────────────────┘ │
│   > Cài đặt  │                                              │
│   > Khiếu nại│                                              │
├──────────────┴──────────────────────────────────────────────┤
│ FOOTER: TrustBridge Market © 2026                           │
└─────────────────────────────────────────────────────────────┘
```

## Testing Checklist

- [ ] Page loads with full dashboard layout
- [ ] Navigation bar displays correctly with wallet balance
- [ ] Sidebar shows user info and highlights Order History
- [ ] Order list renders all orders correctly
- [ ] Order status badges display properly
- [ ] Credential reveal button appears for eligible orders
- [ ] Credentials display correctly when revealed
- [ ] View count increments on credential access
- [ ] User menu dropdown functions
- [ ] Footer displays correctly
- [ ] Responsive layout works on mobile

## Implementation Notes

1. **Reuse Components**: Copy nav, sidebar, and footer sections from buyer_dashboard.html to maintain consistency

2. **Thymeleaf Attributes**: Ensure all `th:` attributes are properly namespaced with `xmlns:th="https://www.thymeleaf.org"`

3. **Active Navigation**: The sidebar should have Order History highlighted using the same pattern as Overview on the dashboard page

4. **Scripts**: Include the user menu toggle script at the bottom of the page

5. **i18n**: Consider extracting text to messages.properties for internationalization consistency
