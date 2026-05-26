package com.example.order_service.client;

import com.example.order_service.client.dto.CartDto;
import com.example.order_service.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", url = "${app.services.cart-service-url}")
public interface CartClient {

    @GetMapping("/api/cart/{userId}")
    ApiResponse<CartDto> getCart(@PathVariable Long userId);

    @DeleteMapping("/api/cart/internal/{userId}/clear")
    ApiResponse<Void> clearCart(@PathVariable Long userId);
}
