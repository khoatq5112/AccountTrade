package com.group3.accounttrade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for creating a new post.
 * Used for form binding and validation in the seller post creation flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostForm {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    /**
     * Rich text content (HTML) from Quill.js editor.
     * Can be null or empty if no description is provided.
     */
    private String description;

    /**
     * Thumbnail image file for Cloudinary upload.
     * Optional - if not provided, the post will use the default generated thumbnail.
     */
    private MultipartFile thumbnailFile;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Integer categoryId;

    // ===== Credentials (Multiple) =====

    /**
     * List of credentials to add to the post.
     * Each credential represents one sellable account.
     * Stock is calculated from the number of credentials.
     */
    @Valid
    @Builder.Default
    private List<CredentialForm> credentials = new ArrayList<>();

    /**
     * Checks if this form has any valid credentials.
     */
    public boolean hasCredentials() {
        if (credentials == null || credentials.isEmpty()) {
            return false;
        }
        return credentials.stream().anyMatch(CredentialForm::hasData);
    }

    /**
     * Gets the count of valid credentials.
     */
    public int getValidCredentialCount() {
        if (credentials == null) {
            return 0;
        }
        return (int) credentials.stream().filter(CredentialForm::hasData).count();
    }
}
