package com.b2c.flash_sale_b2c_UTC2.product.repository;

import com.b2c.flash_sale_b2c_UTC2.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductId(Long productId);
    boolean existsBySku(String sku);
}
