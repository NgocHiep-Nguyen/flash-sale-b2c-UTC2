package com.b2c.flash_sale_b2c_UTC2.cart.repository;

import com.b2c.flash_sale_b2c_UTC2.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);
    List<CartItem> findByCartId(Long cartId);
    void deleteAllByCartId(Long cartId);
}
