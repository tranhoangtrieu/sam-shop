package com.example.product_service.dto.request;

import com.example.product_service.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpdateRequest {
    private String name;

    @DecimalMin("0.0")
    private BigDecimal price;

    private String description;
    private String image;

    @Min(0)
    private Integer quantity;

    private ProductStatus status;
    private Long categoryId;
}
