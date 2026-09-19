package com.b2c.flash_sale_b2c_UTC2.store.dto;

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
public class StoreResponse {
    private Long id;
    private Long userId;
    private String storeName;
    private String logoUrl;
    private String description;
    private BigDecimal defaultCommissionRate;
    private String status;
    private Instant createdAt;
}
