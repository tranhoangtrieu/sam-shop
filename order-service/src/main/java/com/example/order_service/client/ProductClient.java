package com.example.order_service.client;

import com.example.order_service.client.dto.StockUpdateDto;
import com.example.order_service.common.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service", url = "${app.services.product-service-url}")
public interface ProductClient {

    @PutMapping("/api/products/{id}/stock")
    ApiResponse<Object> adjustStock(@PathVariable("id") Long id, @RequestBody StockUpdateDto request);
}
