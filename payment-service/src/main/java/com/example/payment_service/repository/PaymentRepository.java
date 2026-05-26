package com.example.payment_service.repository;

import com.example.payment_service.entity.Payment;
import com.example.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByOrderIdAndPaymentStatusIn(Long orderId, Collection<PaymentStatus> statuses);

    Optional<Payment> findTopByOrderIdAndPaymentStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus paymentStatus);
}
