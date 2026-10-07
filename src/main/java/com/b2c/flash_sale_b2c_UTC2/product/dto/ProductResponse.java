package com.b2c.flash_sale_b2c_UTC2.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private Long storeId;
    private Integer categoryId;
    private String name;
    private String description;
    private java.util.List<TierVariationConfigDto> tierVariationConfigs;
    private String status;
    private Instant createdAt;
}
