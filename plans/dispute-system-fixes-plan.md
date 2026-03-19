# Dispute System Fixes and Enhancements

## Issue 1: Fix Admin Refund Action JSON Error

### Problem

When clicking "Hoàn lại tiền cho người mua", the system returns HTML instead of JSON.

### Root Cause

The `apiPost` function in `admin-dispute-detail.js` doesn't check Content-Type before parsing. When the server returns an HTML error page, `response.json()` fails.

### Solution

Update `admin-dispute-detail.js` to:

1. Check `Content-Type` header before parsing
2. Handle HTML responses gracefully
3. Show proper error messages

### Code Fix

```javascript
async function apiPost(endpoint, params) {
  const headers = {
    "Content-Type": "application/x-www-form-urlencoded",
  };
  headers[getCsrfHeader()] = getCsrfToken();

  const response = await fetch(config.apiBaseUrl + endpoint, {
    method: "POST",
    headers: headers,
    body: params,
    credentials: "same-origin",
  });

  // Check Content-Type before parsing
  const contentType = response.headers.get("Content-Type") || "";

  if (!response.ok) {
    if (contentType.includes("application/json")) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || "Request failed");
    } else {
      // Handle HTML error pages
      const errorText = await response.text().catch(() => "Unknown error");
      console.error("Server returned HTML error:", errorText.substring(0, 200));
      throw new Error(
        "Server error. Please check if you are logged in as admin.",
      );
    }
  }

  if (!contentType.includes("application/json")) {
    throw new Error(
      "Server returned non-JSON response. Please check authentication.",
    );
  }

  return response.json();
}
```

## Issue 2: Show Dispute Evidence Images

### Problem

Evidence images are not displayed in dispute views.

### Solution

1. Update `renderEvidence()` in `admin-dispute-detail.js` to parse and display image URLs
2. Add image preview/expand functionality
3. Update templates to include image containers

### Code Fix for admin-dispute-detail.js

```javascript
function renderEvidence(dispute) {
  // Buyer evidence
  const buyerEvidenceEl = document.getElementById("buyer-evidence");
  const buyerImagesEl = document.getElementById("buyer-evidence-images");

  if (dispute.buyerEvidence) {
    const { text, images } = parseEvidence(dispute.buyerEvidence);
    buyerEvidenceEl.innerHTML =
      '<p class="whitespace-pre-wrap">' + text + "</p>";
    renderEvidenceImages(buyerImagesEl, images, "buyer");
  } else {
    buyerEvidenceEl.innerHTML =
      '<p class="text-gray-400 italic">Chưa có bằng chứng</p>';
    buyerImagesEl.innerHTML = "";
  }

  // Seller response
  const sellerResponseEl = document.getElementById("seller-response");
  const sellerImagesEl = document.getElementById("seller-evidence-images");

  if (dispute.sellerResponse) {
    const { text, images } = parseEvidence(dispute.sellerResponse);
    sellerResponseEl.innerHTML =
      '<p class="whitespace-pre-wrap">' + text + "</p>";
    renderEvidenceImages(sellerImagesEl, images, "seller");
  } else {
    sellerResponseEl.innerHTML =
      '<p class="text-gray-400 italic">Chưa có phản hồi</p>';
    sellerImagesEl.innerHTML = "";
  }
}

function parseEvidence(evidence) {
  if (!evidence) return { text: "", images: [] };

  // Check if evidence contains image URLs (comma-separated)
  const parts = evidence.split(",");
  const images = [];
  const textParts = [];

  parts.forEach((part) => {
    const trimmed = part.trim();
    if (
      trimmed.startsWith("http") &&
      (trimmed.includes("cloudinary") ||
        trimmed.includes(".jpg") ||
        trimmed.includes(".png") ||
        trimmed.includes(".gif"))
    ) {
      images.push(trimmed);
    } else {
      textParts.push(trimmed);
    }
  });

  return {
    text: textParts.join(", "),
    images: images,
  };
}

function renderEvidenceImages(container, images, type) {
  if (!container) return;

  if (images.length === 0) {
    container.classList.add("hidden");
    return;
  }

  container.classList.remove("hidden");
  container.innerHTML = "";

  images.forEach((url, index) => {
    const imgWrapper = document.createElement("div");
    imgWrapper.className = "relative group cursor-pointer";
    imgWrapper.innerHTML = `
            <img src="${url}" 
                 alt="Evidence ${index + 1}" 
                 class="w-full h-24 object-cover rounded-lg border border-gray-200 hover:border-primary transition-colors"
                 onclick="AdminDisputeDetail.showImageModal('${url}')">
            <div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-10 transition-all rounded-lg flex items-center justify-center">
                <i class="ph ph-magnifying-glass text-white text-xl opacity-0 group-hover:opacity-100 transition-opacity"></i>
            </div>
        `;
    container.appendChild(imgWrapper);
  });
}

function showImageModal(imageUrl) {
  const modal = document.createElement("div");
  modal.className =
    "fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 p-4";
  modal.onclick = () => modal.remove();
  modal.innerHTML = `
        <div class="relative max-w-4xl max-h-full">
            <img src="${imageUrl}" alt="Evidence" class="max-w-full max-h-[90vh] object-contain rounded-lg">
            <button class="absolute top-2 right-2 text-white bg-black bg-opacity-50 rounded-full p-2 hover:bg-opacity-75">
                <i class="ph ph-x text-xl"></i>
            </button>
        </div>
    `;
  document.body.appendChild(modal);
}
```

## Issue 3: Add Dispute Management to Seller Dashboard

### Required Changes

#### 1. Update SellerController.java

Add endpoints for:

- `GET /seller/disputes` - List seller's disputes
- `GET /seller/disputes/{id}` - View dispute detail
- `POST /seller/disputes/{id}/response` - Submit seller response

#### 2. Create seller_disputes.html

New template for listing seller's disputes with:

- Order ID
- Product info
- Buyer info
- Dispute reason
- Status
- Created date
- Action buttons

#### 3. Create seller_dispute_detail.html

Template for viewing and responding to disputes with:

- Dispute information
- Buyer evidence
- Response form
- Evidence upload

#### 4. Create seller-disputes.js

JavaScript module for:

- Loading dispute list
- Loading dispute details
- Submitting responses
- Uploading evidence

#### 5. Update seller_dashboard.html

Add sidebar link:

```html
<a
  th:href="@{/seller/disputes}"
  class="flex items-center gap-3 px-4 py-3 text-gray-600 hover:bg-gray-50 hover:text-gray-900 font-bold rounded-lg transition-colors"
>
  <i class="ph ph-gavel text-lg"></i> Quản lý khiếu nại
</a>
```

## Files to Modify

| File                         | Action | Description                         |
| ---------------------------- | ------ | ----------------------------------- |
| `admin-dispute-detail.js`    | MODIFY | Fix JSON parsing, add image support |
| `admin_dispute_detail.html`  | MODIFY | Add image containers                |
| `SellerController.java`      | MODIFY | Add dispute endpoints               |
| `seller_dashboard.html`      | MODIFY | Add sidebar link                    |
| `seller_disputes.html`       | CREATE | New seller disputes list page       |
| `seller_dispute_detail.html` | CREATE | New seller dispute detail page      |
| `seller-disputes.js`         | CREATE | New JavaScript module               |

## Implementation Order

1. Fix admin-dispute-detail.js (Issue 1)
2. Add image support to admin dispute detail (Issue 2)
3. Add seller dispute management (Issue 3)

#### 5. Update seller_dashboard.html

Add sidebar link:

```html
<a
  th:href="@{/seller/disputes}"
  class="flex items-center gap-3 px-4 py-3 text-gray-600 hover:bg-gray-50 hover:text-gray-900 font-bold rounded-lg transition-colors"
>
  <i class="ph ph-gavel text-lg"></i> Quản lý khiếu nại
</a>
```

## Files to Modify

| File                         | Action | Description                         |
| ---------------------------- | ------ | ----------------------------------- |
| `admin-dispute-detail.js`    | MODIFY | Fix JSON parsing, add image support |
| `admin_dispute_detail.html`  | MODIFY | Add image containers                |
| `SellerController.java`      | MODIFY | Add dispute endpoints               |
| `seller_dashboard.html`      | MODIFY | Add sidebar link                    |
| `seller_disputes.html`       | CREATE | New seller disputes list page       |
| `seller_dispute_detail.html` | CREATE | New seller dispute detail page      |
| `seller-disputes.js`         | CREATE | New JavaScript module               |

## Implementation Order

1. Fix admin-dispute-detail.js (Issue 1)
2. Add image support to admin dispute detail (Issue 2)
3. Add seller dispute management (Issue 3)
