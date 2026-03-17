package com.group3.accounttrade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual credential input.
 * Each credential represents one sellable account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialForm {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String accountUsername;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String accountPassword;

    private String securityNotes;

    /**
     * Checks if this form has any data entered.
     * Used to filter out empty credential rows.
     */
    public boolean hasData() {
        return (accountUsername != null && !accountUsername.isBlank()) ||
               (accountPassword != null && !accountPassword.isBlank()) ||
               (securityNotes != null && !securityNotes.isBlank());
    }
}
