package com.b2c.flash_sale_b2c_UTC2.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private Long id;
    private Long storeId;
    private String storeName;
    private Integer categoryId;
    private String categoryName;
    private String name;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private String status;
    private Instant createdAt;
}
