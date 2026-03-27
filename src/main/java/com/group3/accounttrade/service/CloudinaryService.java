package com.group3.accounttrade.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for handling image uploads to Cloudinary.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads an image file to Cloudinary.
     *
     * @param file the image file to upload
     * @return the secure URL of the uploaded image
     * @throws IOException if upload fails
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file hình ảnh");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "account-trade/posts",
                            "resource_type", "image",
                            "use_filename", false,
                            "unique_filename", true,
                            "overwrite", false
                    )
            );

            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Cloudinary upload failed for file {}", file.getOriginalFilename(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected Cloudinary upload failure for file {}", file.getOriginalFilename(), e);
            throw new IOException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an image from Cloudinary by public ID.
     *
     * @param publicId the public ID of the image to delete
     * @throws IOException if deletion fails
     */
    public void deleteImage(String publicId) throws IOException {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    /**
     * Extracts the public ID from a Cloudinary URL.
     *
     * @param url the Cloudinary URL
     * @return the public ID
     */
    public String extractPublicId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // Extract public ID from URL like:
        // https://res.cloudinary.com/cloud-name/image/upload/v123/account-trade/posts/abc123.jpg
        int lastSlash = url.lastIndexOf('/');
        int lastDot = url.lastIndexOf('.');
        if (lastSlash >= 0 && lastDot > lastSlash) {
            return url.substring(lastSlash + 1, lastDot);
        }
        return null;
    }
}
