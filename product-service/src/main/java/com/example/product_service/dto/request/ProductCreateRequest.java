package com.example.product_service.dto.request;

import com.example.product_service.enums.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateRequest {
    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    private String description;
    private String image;

    @NotNull
    @Min(0)
    private Integer quantity;

    private ProductStatus status;
    private Long categoryId;
}
