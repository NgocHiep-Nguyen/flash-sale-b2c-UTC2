package com.b2c.flash_sale_b2c_UTC2.review.repository;

import com.b2c.flash_sale_b2c_UTC2.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Optional<ProductReview> findByOrderItemId(Long orderItemId);
    Page<ProductReview> findByProductIdAndStatus(Long productId, String status, Pageable pageable);
    Page<ProductReview> findByUserId(Long userId, Pageable pageable);
}
