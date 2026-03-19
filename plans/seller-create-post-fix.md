# Fix Plan: Seller Create Post Page Error

## Problem Summary

Accessing `/seller/posts/new` results in a 500 Internal Server Error due to template parsing failure.

**Error Message:**

```
org.thymeleaf.exceptions.TemplateProcessingException: Exception evaluating SpringEL expression:
"#strings.toUpperCase(currentUser.username.substring(0, 2))" (template: "seller_create_post" - line 154, col 39)

Caused by: org.springframework.expression.spel.SpelEvaluationException: EL1007E:
Property or field 'username' cannot be found on null
```

## Root Cause Analysis

The template [`seller_create_post.html`](src/main/resources/templates/seller_create_post.html) references `${currentUser.username}` at:

- **Line 154**: `<span th:text="${#strings.toUpperCase(currentUser.username.substring(0, 2))}">TU</span>`
- **Line 158**: `<h2 class="font-bold text-lg text-gray-900" th:text="${currentUser.username}">Username</h2>`

However, the controller method [`showCreatePostForm()`](src/main/java/com/group3/accounttrade/controller/SellerController.java:113) does not add `currentUser` to the model:

```java
@GetMapping("/posts/new")
public String showCreatePostForm(Model model) {
    model.addAttribute("postForm", new PostForm());
    List<Category> categories = categoryRepository.findAllOrderByDisplayOrderAsc();
    model.addAttribute("categories", categories);
    return "seller_create_post";  // ❌ currentUser not added
}
```

## Pattern Comparison

Other methods in [`SellerController`](src/main/java/com/group3/accounttrade/controller/SellerController.java) correctly add `currentUser`:

| Method                  | Line    | Adds currentUser? |
| ----------------------- | ------- | ----------------- |
| `viewSellerDashboard()` | 49-66   | ✅ Yes            |
| `listSellerOrders()`    | 68-108  | ✅ Yes            |
| `listSellerPosts()`     | 175-243 | ✅ Yes            |
| `showCreatePostForm()`  | 113-122 | ❌ **No**         |

## Solution

Modify [`showCreatePostForm()`](src/main/java/com/group3/accounttrade/controller/SellerController.java:113) to:

1. Accept `Authentication` parameter
2. Look up the current user
3. Add `currentUser` to the model

### Code Change

**File:** [`src/main/java/com/group3/accounttrade/controller/SellerController.java`](src/main/java/com/group3/accounttrade/controller/SellerController.java:113)

**Before:**

```java
@GetMapping("/posts/new")
public String showCreatePostForm(Model model) {
    model.addAttribute("postForm", new PostForm());
    List<Category> categories = categoryRepository.findAllOrderByDisplayOrderAsc();
    model.addAttribute("categories", categories);
    return "seller_create_post";
}
```

**After:**

```java
@GetMapping("/posts/new")
public String showCreatePostForm(Model model, Authentication authentication) {
    // Add current user for sidebar
    String username = authentication.getName();
    User user = userRepository.findByUsername(username).orElse(null);
    model.addAttribute("currentUser", user);

    model.addAttribute("postForm", new PostForm());
    List<Category> categories = categoryRepository.findAllOrderByDisplayOrderAsc();
    model.addAttribute("categories", categories);
    return "seller_create_post";
}
```

## Testing

1. Start the application: `./mvnw spring-boot:run`
2. Log in as a seller user
3. Navigate to `http://localhost:8081/seller/posts/new`
4. Verify the page loads without error
5. Verify the sidebar displays the logged-in user's initials and username
