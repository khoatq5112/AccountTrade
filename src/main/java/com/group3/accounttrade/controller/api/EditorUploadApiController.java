package com.group3.accounttrade.controller.api;

import com.group3.accounttrade.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Slf4j
public class EditorUploadApiController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/editor-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadEditorImages(@RequestParam("images") MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Vui lòng chọn ít nhất 1 hình ảnh."
            ));
        }

        // Validate number of images
        if (images.length > 10) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Chỉ được tải lên tối đa 10 hình ảnh cùng lúc."
            ));
        }

        try {
            List<String> urls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) {
                    continue;
                }
                // Validate content type
                String contentType = image.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    continue;
                }
                urls.add(cloudinaryService.uploadImage(image));
            }

            if (urls.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Không có hình ảnh hợp lệ để tải lên."
                ));
            }

            return ResponseEntity.ok(Map.of("urls", urls));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            log.error("Editor image upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Tải ảnh lên thất bại. Vui lòng thử lại với ảnh nhỏ hơn hoặc thử lại sau."
            ));
        } catch (Exception e) {
            log.error("Unexpected editor image upload failure", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Không thể tải ảnh trong lúc này. Vui lòng thử lại sau ít phút."
            ));
        }
    }
}
