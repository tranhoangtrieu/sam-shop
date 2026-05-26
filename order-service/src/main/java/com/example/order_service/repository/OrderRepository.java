package com.example.order_service.repository;

import com.example.order_service.entity.Order;
import com.example.order_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByOrderByCreatedAtDesc();

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.paymentStatus = com.example.order_service.enums.PaymentStatus.PAID")
    BigDecimal sumTotalRevenue();
}
