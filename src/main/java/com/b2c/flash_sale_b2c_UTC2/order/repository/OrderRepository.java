package com.b2c.flash_sale_b2c_UTC2.order.repository;

import com.b2c.flash_sale_b2c_UTC2.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);
    Page<Order> findByStoreId(Long storeId, Pageable pageable);
}
