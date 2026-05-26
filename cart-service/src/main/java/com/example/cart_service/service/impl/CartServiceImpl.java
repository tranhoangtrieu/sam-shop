package com.example.cart_service.service.impl;

import com.example.cart_service.client.ProductClient;
import com.example.cart_service.common.ApiResponse;
import com.example.cart_service.dto.response.CartResponse;
import com.example.cart_service.dto.response.ProductResponse;
import com.example.cart_service.entity.Cart;
import com.example.cart_service.entity.CartItem;
import com.example.cart_service.enums.ProductStatus;
import com.example.cart_service.exception.AppException;
import com.example.cart_service.exception.ErrorCode;
import com.example.cart_service.mapper.CartMapper;
import com.example.cart_service.repository.CartItemRepository;
import com.example.cart_service.repository.CartRepository;
import com.example.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, Long productId, Integer quantity) {
        ProductResponse product = fetchProduct(productId);
        validateProductAvailable(product);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        int newQuantity = existingItem == null ? quantity : existingItem.getQuantity() + quantity;
        ensureStock(product, newQuantity);

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            existingItem.setSubtotal(calculateSubtotal(existingItem.getPrice(), newQuantity));
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(quantity)
                    .subtotal(calculateSubtotal(product.getPrice(), quantity))
                    .build();
            cart.getItems().add(item);
        }

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder().userId(userId).build());
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, Integer quantity) {
        CartItem item = cartItemRepository.findByIdAndCart_UserId(itemId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        ProductResponse product = fetchProduct(item.getProductId());
        validateProductAvailable(product);
        ensureStock(product, quantity);

        item.setQuantity(quantity);
        item.setSubtotal(calculateSubtotal(item.getPrice(), quantity));
        return cartMapper.toResponse(item.getCart());
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndCart_UserId(itemId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private ProductResponse fetchProduct(Long productId) {
        try {
            ApiResponse<ProductResponse> response = productClient.getProduct(productId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            return response.getData();
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Unable to fetch product: " + productId);
        }
    }

    private void validateProductAvailable(ProductResponse product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
    }

    private void ensureStock(ProductResponse product, int requestedQuantity) {
        if (product.getQuantity() == null || product.getQuantity() < requestedQuantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private BigDecimal calculateSubtotal(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
