package com.example.payment_service.service;

import com.example.payment_service.dto.response.OnlineInitiateResponse;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentResponse createPayment(Long orderId, String userId, BigDecimal amount, PaymentMethod paymentMethod);

    PaymentResponse getByOrderId(Long orderId);

    PaymentResponse confirmCod(Long paymentId);

    OnlineInitiateResponse initiateOnline(Long paymentId);

    PaymentResponse processOnlineCallback(Long paymentId, boolean success);

    PaymentResponse retryOnline(Long orderId, String userId);
}
