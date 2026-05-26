package com.example.order_service.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartDto {
    private Long cartId;
    private Long userId;
    private List<CartItemDto> items;
    private BigDecimal totalPrice;
}
