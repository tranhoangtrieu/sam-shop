package com.example.payment_service.service.impl;

import com.example.payment_service.client.OrderClient;
import com.example.payment_service.client.dto.UpdateOrderPaymentStatusDto;
import com.example.payment_service.entity.Payment;
import com.example.payment_service.enums.PaymentMethod;
import com.example.payment_service.enums.PaymentStatus;
import com.example.payment_service.exception.AppException;
import com.example.payment_service.exception.ErrorCode;
import com.example.payment_service.mapper.PaymentMapper;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.dto.response.OnlineInitiateResponse;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.service.PaymentService;
import com.example.payment_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final List<PaymentStatus> ACTIVE_STATUSES = List.of(PaymentStatus.UNPAID, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderClient orderClient;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long orderId, String userId, BigDecimal amount, PaymentMethod paymentMethod) {
        if (paymentRepository.existsByOrderIdAndPaymentStatusIn(orderId, ACTIVE_STATUSES)) {
            throw new AppException(ErrorCode.DUPLICATE_PAYMENT);
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        assertCanAccessPayment(payment);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmCod(Long paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);

        if (payment.getPaymentMethod() != PaymentMethod.COD) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }
        if (payment.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_STATE, "COD payment must be UNPAID to confirm");
        }

        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        Payment saved = paymentRepository.save(payment);
        syncOrderPaymentStatus(saved.getOrderId(), PaymentStatus.PAID);
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OnlineInitiateResponse initiateOnline(Long paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        assertCanAccessPayment(payment);

        if (payment.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }
        if (payment.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_STATE, "Online payment must be UNPAID to initiate");
        }

        String token = UUID.randomUUID().toString();
        String paymentUrl = "https://mock-payment.sam-shop.local/checkout?paymentId="
                + paymentId + "&token=" + token;

        return OnlineInitiateResponse.builder()
                .paymentId(paymentId)
                .paymentUrl(paymentUrl)
                .token(token)
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse processOnlineCallback(Long paymentId, boolean success) {
        Payment payment = findPaymentOrThrow(paymentId);

        if (payment.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }
        if (payment.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_STATE, "Online payment must be UNPAID for callback");
        }

        if (success) {
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setTransactionCode("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
            payment.setPaidAt(Instant.now());
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
        }

        Payment saved = paymentRepository.save(payment);
        if (success) {
            syncOrderPaymentStatus(saved.getOrderId(), PaymentStatus.PAID);
        }
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse retryOnline(Long orderId, String userId) {
        Payment latest = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!latest.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "You can only retry your own order payments");
        }
        if (latest.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD, "Retry is only available for ONLINE payments");
        }
        if (latest.getPaymentStatus() != PaymentStatus.FAILED) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_STATE, "Only FAILED online payments can be retried");
        }
        if (paymentRepository.existsByOrderIdAndPaymentStatusIn(orderId, ACTIVE_STATUSES)) {
            throw new AppException(ErrorCode.DUPLICATE_PAYMENT);
        }

        Payment retry = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(latest.getAmount())
                .paymentMethod(PaymentMethod.ONLINE)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();

        return paymentMapper.toResponse(paymentRepository.save(retry));
    }

    private Payment findPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void syncOrderPaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        orderClient.updatePaymentStatus(
                orderId,
                UpdateOrderPaymentStatusDto.builder().paymentStatus(paymentStatus).build());
    }

    private void assertCanAccessPayment(Payment payment) {
        if (SecurityUtils.hasAnyRole("EMPLOYEE", "ADMIN")) {
            return;
        }
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(payment.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "You can only view your own order payments");
        }
    }
}
