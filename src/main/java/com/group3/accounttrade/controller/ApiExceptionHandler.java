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

/**
 * Global exception handler for REST API controllers.
 * Handles exceptions from @RestController classes and returns JSON responses.
 * This complements GlobalExceptionHandler which only handles @Controller classes.
 */
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
