package com.example.payment_service.controller;

import com.example.payment_service.common.ApiResponse;
import com.example.payment_service.dto.request.CreatePaymentRequest;
import com.example.payment_service.dto.request.InitiateOnlineRequest;
import com.example.payment_service.dto.request.OnlineCallbackRequest;
import com.example.payment_service.dto.request.RetryOnlineRequest;
import com.example.payment_service.dto.response.OnlineInitiateResponse;
import com.example.payment_service.dto.response.PaymentResponse;
import com.example.payment_service.exception.AppException;
import com.example.payment_service.exception.ErrorCode;
import com.example.payment_service.service.PaymentService;
import com.example.payment_service.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/order/{orderId}")
    public ApiResponse<PaymentResponse> getByOrderId(@PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.getByOrderId(orderId));
    }

    @PostMapping("/internal/create")
    public ApiResponse<PaymentResponse> createInternal(@Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(paymentService.createPayment(
                request.getOrderId(),
                request.getUserId(),
                request.getAmount(),
                request.getPaymentMethod()));
    }

    @PutMapping("/{id}/confirm-cod")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ApiResponse<PaymentResponse> confirmCod(@PathVariable Long id) {
        return ApiResponse.ok(paymentService.confirmCod(id));
    }

    @PostMapping("/online/initiate")
    public ApiResponse<OnlineInitiateResponse> initiateOnline(@Valid @RequestBody InitiateOnlineRequest request) {
        return ApiResponse.ok(paymentService.initiateOnline(request.getPaymentId()));
    }

    @PostMapping("/online/callback")
    public ApiResponse<PaymentResponse> onlineCallback(@Valid @RequestBody OnlineCallbackRequest request) {
        return ApiResponse.ok(paymentService.processOnlineCallback(
                request.getPaymentId(),
                Boolean.TRUE.equals(request.getSuccess())));
    }

    @PostMapping("/online/retry")
    public ApiResponse<PaymentResponse> retryOnline(@Valid @RequestBody RetryOnlineRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(paymentService.retryOnline(request.getOrderId(), userId));
    }
}
