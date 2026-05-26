package com.example.payment_service.client;

import com.example.payment_service.client.dto.UpdateOrderPaymentStatusDto;
import com.example.payment_service.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service", url = "${app.services.order-service-url}")
public interface OrderClient {

    @PutMapping("/api/orders/internal/{orderId}/payment-status")
    ApiResponse<Object> updatePaymentStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody UpdateOrderPaymentStatusDto request);
}
