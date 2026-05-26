package com.example.order_service.dto.request;

import com.example.order_service.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotBlank
    private String shippingAddress;

    @NotNull
    private PaymentMethod paymentMethod;
}
