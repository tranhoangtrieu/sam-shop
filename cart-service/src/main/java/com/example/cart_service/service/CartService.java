package com.example.cart_service.service;

import com.example.cart_service.dto.response.CartResponse;

public interface CartService {

    CartResponse addToCart(Long userId, Long productId, Integer quantity);

    CartResponse getCart(Long userId);

    CartResponse updateItem(Long userId, Long itemId, Integer quantity);

    CartResponse removeItem(Long userId, Long itemId);

    void clearCart(Long userId);
}
