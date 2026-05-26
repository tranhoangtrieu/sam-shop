package com.example.product_service.service;

import com.example.product_service.config.UploadProperties;
import com.example.product_service.exception.AppException;
import com.example.product_service.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductImageStorageService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final UploadProperties uploadProperties;

    public String store(Long productId, MultipartFile file) {
        validate(file);
        String filename = buildFilename(productId, file.getContentType());
        Path target = uploadProperties.resolveDir().resolve(filename);
        try {
            file.transferTo(target);
            return uploadProperties.publicUrl(filename);
        } catch (IOException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Failed to save image file");
        }
    }

    public Resource loadAsResource(String filename) {
        Path file = resolveExistingFile(filename);
        return new FileSystemResource(file);
    }

    public void deleteIfExists(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        String filename = extractFilename(imageUrl);
        if (filename == null) {
            return;
        }
        try {
            Path file = uploadProperties.resolveDir().resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // ignore cleanup errors
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Only JPEG, PNG, WebP or GIF images are allowed");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Image must be at most 2MB");
        }
    }

    private String buildFilename(Long productId, String contentType) {
        String ext = EXTENSIONS.getOrDefault(contentType, ".jpg");
        return "product-" + productId + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
    }

    private Path resolveExistingFile(String filename) {
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Invalid image filename");
        }
        Path file = uploadProperties.resolveDir().resolve(filename).normalize();
        if (!file.startsWith(uploadProperties.resolveDir()) || !Files.isRegularFile(file)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Image not found");
        }
        return file;
    }

    private String extractFilename(String imageUrl) {
        String prefix = uploadProperties.getPublicPath();
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        if (imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        int slash = imageUrl.lastIndexOf('/');
        if (slash >= 0 && slash < imageUrl.length() - 1) {
            return imageUrl.substring(slash + 1);
        }
        return null;
    }
}
