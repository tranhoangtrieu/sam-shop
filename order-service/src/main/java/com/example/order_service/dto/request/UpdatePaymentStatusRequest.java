package com.example.order_service.dto.request;

import com.example.order_service.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePaymentStatusRequest {

    @NotNull
    private PaymentStatus paymentStatus;
}
