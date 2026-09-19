package com.b2c.flash_sale_b2c_UTC2.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private Long productId;
    private String sku;
    private String variantName;
    private String attributes;
    private BigDecimal originalPrice;
    private Integer stockQuantity;
    private String imageUrl;
    private String status;
    private Instant createdAt;
}
