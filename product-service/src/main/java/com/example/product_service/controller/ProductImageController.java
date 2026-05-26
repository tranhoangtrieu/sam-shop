package com.example.product_service.controller;

import com.example.product_service.service.ProductImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/uploads")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageStorageService imageStorageService;

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        Resource resource = imageStorageService.loadAsResource(filename);
        String contentType = MediaType.IMAGE_JPEG_VALUE;
        String name = filename.toLowerCase();
        if (name.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (name.endsWith(".webp")) {
            contentType = "image/webp";
        } else if (name.endsWith(".gif")) {
            contentType = MediaType.IMAGE_GIF_VALUE;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
