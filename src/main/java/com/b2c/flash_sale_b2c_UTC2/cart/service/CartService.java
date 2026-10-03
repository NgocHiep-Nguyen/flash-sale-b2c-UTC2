package com.b2c.flash_sale_b2c_UTC2.cart.service;

import com.b2c.flash_sale_b2c_UTC2.cart.dto.AddToCartRequest;
import com.b2c.flash_sale_b2c_UTC2.cart.dto.CartResponse;
import com.b2c.flash_sale_b2c_UTC2.cart.dto.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request);

    void removeCartItem(Long userId, Long itemId);

    void clearCart(Long userId);
}
