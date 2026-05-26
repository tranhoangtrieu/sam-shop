package com.example.order_service.client.dto;

import com.example.order_service.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentDto {
    private Long orderId;
    private String userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
}
