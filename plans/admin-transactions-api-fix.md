# Admin Transactions API Fix Plan

## Problem

The admin-transactions.js is receiving an HTML response instead of JSON when calling `/api/admin/transactions`:

```
Server returned non-JSON response. Content-Type: text/html;charset=UTF-8
Error fetching transactions: Error: Server trả về phản hồi không hợp lệ. Vui lòng thử lại.
```

## Root Cause Analysis

The [`GlobalExceptionHandler`](../src/main/java/com/group3/accounttrade/controller/GlobalExceptionHandler.java) class is annotated with:

```java
@ControllerAdvice(annotations = Controller.class)
```

This means it **only handles exceptions from controllers annotated with `@Controller`**.

However, [`AdminDashboardController`](../src/main/java/com/group3/accounttrade/controller/AdminDashboardController.java) is annotated with `@RestController`. When an exception occurs in the `/api/admin/transactions` endpoint:

1. The `GlobalExceptionHandler` doesn't catch it (because it only handles `@Controller`)
2. Spring Boot's default error handling kicks in
3. Default error handling returns an HTML error page
4. The frontend receives `text/html;charset=UTF-8` instead of `application/json`

## Solution

Create a new `ApiExceptionHandler` class with `@RestControllerAdvice` to handle exceptions from REST controllers and return proper JSON error responses.

### Implementation

Create file: `src/main/java/com/group3/accounttrade/controller/ApiExceptionHandler.java`

```java
package com.group3.accounttrade.controller;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "com.group3.accounttrade.controller")
public class ApiExceptionHandler {

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<Map<String, Object>> handleLazyInitializationException(
            LazyInitializationException exception, WebRequest request) {
        log.error("[API-ERROR] LazyInitializationException: {}", exception.getMessage(), exception);
        return createErrorResponse("Lỗi tải dữ liệu. Vui lòng thử lại.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException exception, WebRequest request) {
        log.error("[API-ERROR] AccessDeniedException: {}", exception.getMessage());
        return createErrorResponse("Bạn không có quyền thực hiện thao tác này.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException exception, WebRequest request) {
        log.error("[API-ERROR] IllegalArgumentException: {}", exception.getMessage());
        return createErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException exception, WebRequest request) {
        log.error("[API-ERROR] IllegalStateException: {}", exception.getMessage());
        return createErrorResponse(exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception exception, WebRequest request) {
        log.error("[API-ERROR] Unhandled exception: {}", exception.getMessage(), exception);
        return createErrorResponse("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.", 
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
}
```

## Benefits

1. **Proper JSON responses**: All API endpoints will return JSON errors instead of HTML
2. **Consistent error format**: All errors follow the same structure with `success`, `message`, and `timestamp`
3. **Proper HTTP status codes**: Different exception types return appropriate status codes
4. **Logging**: All exceptions are logged for debugging

## Files to Create

| File | Action |
|------|--------|
| `src/main/java/com/group3/accounttrade/controller/ApiExceptionHandler.java` | Create new |

## Testing

After implementing the fix:
1. Restart the application
2. Navigate to the admin transactions page
3. Verify that transactions load correctly
4. Test error scenarios to ensure JSON error responses are returned
