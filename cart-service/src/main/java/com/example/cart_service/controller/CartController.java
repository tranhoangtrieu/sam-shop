package com.example.cart_service.controller;

import com.example.cart_service.common.ApiResponse;
import com.example.cart_service.dto.request.AddToCartRequest;
import com.example.cart_service.dto.request.UpdateCartItemRequest;
import com.example.cart_service.dto.response.CartResponse;
import com.example.cart_service.service.CartService;
import com.example.cart_service.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ApiResponse<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(cartService.addToCart(userId, request.getProductId(), request.getQuantity()));
    }

    @GetMapping("/{userId}")
    public ApiResponse<CartResponse> getCart(@PathVariable Long userId) {
        SecurityUtils.assertCurrentUser(userId);
        return ApiResponse.ok(cartService.getCart(userId));
    }

    @PutMapping("/update")
    public ApiResponse<CartResponse> updateItem(@Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(cartService.updateItem(userId, request.getItemId(), request.getQuantity()));
    }

    @DeleteMapping("/remove/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long itemId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(cartService.removeItem(userId, itemId));
    }

    @DeleteMapping("/internal/{userId}/clear")
    public ApiResponse<Void> clearCartInternal(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ApiResponse.ok("Cart cleared", null);
    }
}
