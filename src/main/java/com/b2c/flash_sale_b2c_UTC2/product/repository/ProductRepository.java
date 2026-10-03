package com.b2c.flash_sale_b2c_UTC2.product.repository;

import com.b2c.flash_sale_b2c_UTC2.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
        SELECT DISTINCT p FROM Product p
        JOIN FETCH p.store s
        JOIN FETCH p.category c
        WHERE p.status = 'ACTIVE'
        AND s.status = 'APPROVED'
        AND (:categoryId IS NULL OR c.id = :categoryId)
        AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minPrice IS NULL OR EXISTS (
            SELECT 1 FROM ProductVariant v WHERE v.product.id = p.id AND v.status = 'ACTIVE' AND v.originalPrice >= :minPrice
        ))
        AND (:maxPrice IS NULL OR EXISTS (
            SELECT 1 FROM ProductVariant v WHERE v.product.id = p.id AND v.status = 'ACTIVE' AND v.originalPrice <= :maxPrice
        ))
    """,
    countQuery = """
        SELECT count(DISTINCT p) FROM Product p
        WHERE p.status = 'ACTIVE'
        AND p.store.status = 'APPROVED'
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:minPrice IS NULL OR EXISTS (
            SELECT 1 FROM ProductVariant v WHERE v.product.id = p.id AND v.status = 'ACTIVE' AND v.originalPrice >= :minPrice
        ))
        AND (:maxPrice IS NULL OR EXISTS (
            SELECT 1 FROM ProductVariant v WHERE v.product.id = p.id AND v.status = 'ACTIVE' AND v.originalPrice <= :maxPrice
        ))
    """)
    Page<Product> findPublicProducts(
            @Param("categoryId") Integer categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query(value = """
        SELECT p FROM Product p
        JOIN FETCH p.store s
        JOIN FETCH p.category c
        WHERE s.id = :storeId
        AND (:status IS NULL OR p.status = :status)
    """,
    countQuery = """
        SELECT count(p) FROM Product p
        WHERE p.store.id = :storeId
        AND (:status IS NULL OR p.status = :status)
    """)
    Page<Product> findSellerProducts(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p JOIN FETCH p.store s JOIN FETCH p.category c WHERE p.id = :id")
    Optional<Product> findByIdWithStoreAndCategory(@Param("id") Long id);
}
