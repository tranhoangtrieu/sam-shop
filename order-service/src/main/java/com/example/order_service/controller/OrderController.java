package com.example.order_service.controller;

import com.example.order_service.common.ApiResponse;
import com.example.order_service.dto.request.CreateOrderRequest;
import com.example.order_service.dto.request.UpdateOrderStatusRequest;
import com.example.order_service.dto.request.UpdatePaymentStatusRequest;
import com.example.order_service.dto.response.OrderResponse;
import com.example.order_service.dto.response.RevenueResponse;
import com.example.order_service.exception.AppException;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.service.OrderService;
import com.example.order_service.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = requireUserId();
        return ApiResponse.ok(orderService.createOrder(
                userId,
                request.getShippingAddress(),
                request.getPaymentMethod()));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> myOrders() {
        Long userId = requireUserId();
        return ApiResponse.ok(orderService.myOrders(userId));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RevenueResponse> getRevenue() {
        return ApiResponse.ok(orderService.getRevenue());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ApiResponse<List<OrderResponse>> getAllOrders() {
        return ApiResponse.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@PathVariable Long id) {
        Long userId = requireUserId();
        return ApiResponse.ok(orderService.getById(id, userId, SecurityUtils.isStaff()));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ApiResponse<OrderResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(orderService.confirm(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateStatus(id, request.getStatus()));
    }

    @PutMapping("/internal/{id}/payment-status")
    public ApiResponse<OrderResponse> updatePaymentStatusInternal(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        return ApiResponse.ok(orderService.updatePaymentStatus(id, request.getPaymentStatus()));
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User id not found in token");
        }
        return userId;
    }

}
