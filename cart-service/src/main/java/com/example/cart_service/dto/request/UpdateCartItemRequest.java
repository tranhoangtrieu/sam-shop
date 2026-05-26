package com.example.cart_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCartItemRequest {

    @NotNull
    private Long itemId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
