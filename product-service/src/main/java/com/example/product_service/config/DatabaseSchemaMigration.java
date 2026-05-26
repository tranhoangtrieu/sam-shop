package com.example.product_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Ensures products.image can store URL paths (VARCHAR 512). */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateImageColumn() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE products ALTER COLUMN image TYPE VARCHAR(512)");
            log.info("products.image column is VARCHAR(512) for image URL paths");
        } catch (Exception e) {
            log.warn("products.image migration skipped: {}", e.getMessage());
        }
    }
}
