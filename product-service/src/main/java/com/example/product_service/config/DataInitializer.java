package com.example.product_service.config;

import com.example.product_service.entity.Category;
import com.example.product_service.entity.Product;
import com.example.product_service.enums.ProductStatus;
import com.example.product_service.repository.CategoryRepository;
import com.example.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            if (productRepository.count() > 0) return;

            Category electronics = categoryRepository.save(Category.builder()
                    .name("Điện tử")
                    .description("Thiết bị điện tử")
                    .build());
            Category fashion = categoryRepository.save(Category.builder()
                    .name("Thời trang")
                    .description("Quần áo, phụ kiện")
                    .build());

            productRepository.save(Product.builder()
                    .name("Tai nghe Bluetooth")
                    .price(new BigDecimal("599000"))
                    .description("Tai nghe không dây chống ồn")
                    .quantity(50)
                    .status(ProductStatus.ACTIVE)
                    .category(electronics)
                    .build());
            productRepository.save(Product.builder()
                    .name("Áo thun Sam Shop")
                    .price(new BigDecimal("199000"))
                    .description("Áo cotton 100%")
                    .quantity(100)
                    .status(ProductStatus.ACTIVE)
                    .category(fashion)
                    .build());
        };
    }
}
