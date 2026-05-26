package com.example.order_service.service.impl;

import com.example.order_service.client.CartClient;
import com.example.order_service.client.PaymentClient;
import com.example.order_service.client.ProductClient;
import com.example.order_service.client.dto.CartDto;
import com.example.order_service.client.dto.CartItemDto;
import com.example.order_service.client.dto.CreatePaymentDto;
import com.example.order_service.client.dto.StockUpdateDto;
import com.example.order_service.common.ApiResponse;
import com.example.order_service.dto.response.OrderResponse;
import com.example.order_service.dto.response.RevenueResponse;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderItem;
import com.example.order_service.enums.OrderStatus;
import com.example.order_service.enums.PaymentMethod;
import com.example.order_service.enums.PaymentStatus;
import com.example.order_service.exception.AppException;
import com.example.order_service.exception.ErrorCode;
import com.example.order_service.mapper.OrderMapper;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final Set<OrderStatus> SHIPPING_OR_COMPLETED =
            EnumSet.of(OrderStatus.SHIPPING, OrderStatus.COMPLETED);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, String shippingAddress, PaymentMethod paymentMethod) {
        CartDto cart = unwrap(cartClient.getCart(userId), "Failed to load cart");
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_CART);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItemDto cartItem : cart.getItems()) {
            unwrap(
                    productClient.adjustStock(
                            cartItem.getProductId(),
                            new StockUpdateDto(-cartItem.getQuantity())),
                    "Failed to adjust stock for product " + cartItem.getProductId());

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            orderItems.add(orderItem);
            totalPrice = totalPrice.add(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        Order order = Order.builder()
                .userId(userId)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(paymentMethod)
                .shippingAddress(shippingAddress)
                .build();

        orderItems.forEach(order::addItem);
        Order saved = orderRepository.save(order);

        CreatePaymentDto paymentRequest = CreatePaymentDto.builder()
                .orderId(saved.getId())
                .userId(String.valueOf(userId))
                .amount(saved.getTotalPrice())
                .paymentMethod(paymentMethod)
                .build();
        unwrap(paymentClient.createPayment(paymentRequest), "Failed to create payment");

        unwrap(cartClient.clearCart(userId), "Failed to clear cart");

        log.info("Order {} created for user {}", saved.getId(), userId);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(Long userId) {
        return orderMapper.toResponseList(orderRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id, Long userId, boolean isStaff) {
        Order order = getOrder(id);
        if (!isStaff && !order.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "You cannot access this order");
        }
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderMapper.toResponseList(orderRepository.findAllByOrderByCreatedAtDesc());
    }

    @Override
    @Transactional
    public OrderResponse confirm(Long id) {
        Order order = getOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_STATUS, "Only pending orders can be confirmed");
        }
        order.setStatus(OrderStatus.CONFIRMED);
        log.info("Order {} confirmed", id);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = getOrder(id);
        validateStatusTransition(order.getStatus(), newStatus);
        enforcePaymentBeforeShipping(order, newStatus);

        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(newStatus);
        log.info("Order {} status updated to {}", id, newStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
        Order order = getOrder(id);
        order.setPaymentStatus(paymentStatus);
        log.info("Order {} payment status updated to {}", id, paymentStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueResponse getRevenue() {
        return RevenueResponse.builder()
                .totalRevenue(orderRepository.sumTotalRevenue())
                .paidOrderCount(orderRepository.countByPaymentStatus(PaymentStatus.PAID))
                .pendingOrderCount(orderRepository.countByPaymentStatus(PaymentStatus.UNPAID))
                .build();
    }

    private void enforcePaymentBeforeShipping(Order order, OrderStatus newStatus) {
        if (!SHIPPING_OR_COMPLETED.contains(newStatus)) {
            return;
        }
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            return;
        }
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new AppException(
                    ErrorCode.PAYMENT_REQUIRED,
                    "Online orders must be paid before shipping or completion");
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == next) {
            return;
        }
        if (current == OrderStatus.CANCELLED || current == OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_STATUS, "Order cannot change from " + current);
        }

        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.COMPLETED;
            default -> false;
        };

        if (!valid) {
            throw new AppException(
                    ErrorCode.INVALID_STATUS,
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productClient.adjustStock(item.getProductId(), new StockUpdateDto(item.getQuantity()));
        }
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Order not found"));
    }

    private <T> T unwrap(ApiResponse<T> response, String errorMessage) {
        if (response == null || !response.isSuccess()) {
            String message = response != null && response.getMessage() != null
                    ? response.getMessage()
                    : errorMessage;
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, message);
        }
        return response.getData();
    }
}
