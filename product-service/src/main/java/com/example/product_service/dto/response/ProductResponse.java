package com.example.product_service.dto.response;

import com.example.product_service.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
    private String image;
    private Integer quantity;
    private ProductStatus status;
    private Long categoryId;
    private String categoryName;
    private Instant createdAt;
}
