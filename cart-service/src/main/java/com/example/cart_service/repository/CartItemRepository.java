package com.example.cart_service.repository;

import com.example.cart_service.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCart_UserId(Long id, Long userId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}
