package com.example.order_service.client;

import com.example.order_service.client.dto.CreatePaymentDto;
import com.example.order_service.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${app.services.payment-service-url}")
public interface PaymentClient {

    @PostMapping("/api/payments/internal/create")
    ApiResponse<Object> createPayment(@RequestBody CreatePaymentDto request);
}
