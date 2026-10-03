package com.b2c.flash_sale_b2c_UTC2.product.repository;

import com.b2c.flash_sale_b2c_UTC2.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByProductId(Long productId);
    List<ProductVariant> findByProductIdAndStatus(Long productId, String status);
    List<ProductVariant> findByProductIdInAndStatus(List<Long> productIds, String status);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = :newStock WHERE v.id = :id AND v.stockQuantity = :expectedOldStock")
    int updateStockQuantityConditionally(@Param("id") Long id, @Param("newStock") Integer newStock, @Param("expectedOldStock") Integer expectedOldStock);
}
