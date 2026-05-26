package com.example.order_service.service;

import com.example.order_service.dto.request.CreateOrderRequest;
import com.example.order_service.dto.response.OrderResponse;
import com.example.order_service.dto.response.RevenueResponse;
import com.example.order_service.enums.OrderStatus;
import com.example.order_service.enums.PaymentMethod;
import com.example.order_service.enums.PaymentStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(Long userId, String shippingAddress, PaymentMethod paymentMethod);

    List<OrderResponse> myOrders(Long userId);

    OrderResponse getById(Long id, Long userId, boolean isStaff);

    List<OrderResponse> getAllOrders();

    OrderResponse confirm(Long id);

    OrderResponse updateStatus(Long id, OrderStatus status);

    RevenueResponse getRevenue();

    OrderResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus);
}
