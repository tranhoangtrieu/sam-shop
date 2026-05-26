package com.example.cart_service.dto.response;

import com.example.cart_service.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private ProductStatus status;
}
