package com.b2c.flash_sale_b2c_UTC2.cart.repository;

import com.b2c.flash_sale_b2c_UTC2.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}
