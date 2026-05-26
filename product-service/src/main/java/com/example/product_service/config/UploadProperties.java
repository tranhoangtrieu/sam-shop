package com.example.product_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** Directory on disk (e.g. uploads or /app/uploads). */
    private String dir = "uploads";

    /** Public URL prefix stored in DB and used by the frontend. */
    private String publicPath = "/api/products/uploads";

    public Path resolveDir() {
        return Paths.get(dir).toAbsolutePath().normalize();
    }

    public String publicUrl(String filename) {
        String base = publicPath.endsWith("/") ? publicPath.substring(0, publicPath.length() - 1) : publicPath;
        return base + "/" + filename;
    }
}
