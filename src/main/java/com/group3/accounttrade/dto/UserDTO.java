package com.group3.accounttrade.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for user information in admin dashboard.
 */
public record UserDTO(
    Integer userId,
    String username,
    String email,
    String roleName,
    boolean active,
    LocalDateTime createdAt
) {
}
