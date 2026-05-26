package com.example.product_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;

@Configuration
@EnableConfigurationProperties(UploadProperties.class)
@RequiredArgsConstructor
@Slf4j
public class UploadConfig {

    private final UploadProperties uploadProperties;

    @PostConstruct
    void ensureUploadDirectory() throws IOException {
        var dir = uploadProperties.resolveDir();
        Files.createDirectories(dir);
        log.info("Product image upload directory: {}", dir);
    }
}
